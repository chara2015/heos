package heos.utils;

import heos.Heos;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.filter.AbstractFilter;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LogFilterService {
    private static final Filter UPDATE_SUPPRESSION_FILTER = new UpdateSuppressionFilter();
    private static boolean installed;
    private static long lastCrashNoticeMillis;

    private LogFilterService() {
    }

    public static void installConfiguredFilters() {
        if (!Heos.getConfig().enableLogFilter || installed) {
            return;
        }

        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        Configuration configuration = context.getConfiguration();
        LoggerConfig rootLogger = configuration.getRootLogger();
        rootLogger.addFilter(UPDATE_SUPPRESSION_FILTER);
        context.updateLoggers();
        installed = true;
        HeosLogger.info("日志过滤: Enabled");
    }

    private static final class UpdateSuppressionFilter extends AbstractFilter {
        private static final String PACKET_ERROR = "failed to handle packet";
        private static final String UPDATE_NEIGHBORS = "exception while updating neighbours";
        private static final String CAUSED_SERVER_CRASH = "you just caused a server crash";
        private static final String UPDATE_SUPPRESSION = "update suppression";
        private static final String STACK_OVERFLOW_SUPPRESSION = "stackoverflowsuppression";
        private static final String TIS_UPDATE_SUPPRESSION = "yeetupdatesuppressioncrash";
        private static final long STACK_SUMMARY_SUPPRESSION_MILLIS = 1000L;
        private static final Pattern BLOCK_POSITION = Pattern.compile("\\[[\\-0-9]+,\\s*[\\-0-9]+,\\s*[\\-0-9]+\\]");

        @Override
        public Result filter(LogEvent event) {
            if (!Heos.getConfig().enableLogFilter) {
                return Result.NEUTRAL;
            }

            String message = event.getMessage() == null ? "" : event.getMessage().getFormattedMessage();
            Throwable thrown = event.getThrown();
            if (isUpdateSuppressionCrashNotice(message)) {
                lastCrashNoticeMillis = System.currentTimeMillis();
                HeosLogger.warn(Messages.updateSuppressionCrash(updateSuppressionDetail(message)));
                return Result.DENY;
            }
            if (matches(message, thrown)) {
                String detail = updateSuppressionPosition(message, thrown);
                if (!recentCrashNotice()) {
                    HeosLogger.warn(Messages.updateSuppressionCrash(detail));
                }
                return Result.DENY;
            }
            return Result.NEUTRAL;
        }

        private boolean matches(String message, Throwable thrown) {
            String normalizedMessage = message.toLowerCase(Locale.ROOT);
            if (normalizedMessage.contains(PACKET_ERROR) && hasUpdateSuppressionCause(thrown)) {
                return true;
            }
            return normalizedMessage.contains(UPDATE_NEIGHBORS) && hasUpdateSuppressionCause(thrown);
        }

        private boolean isUpdateSuppressionCrashNotice(String message) {
            String normalizedMessage = message.toLowerCase(Locale.ROOT);
            return normalizedMessage.contains(CAUSED_SERVER_CRASH) && normalizedMessage.contains(UPDATE_SUPPRESSION);
        }

        private String updateSuppressionDetail(String message) {
            return updateSuppressionPosition(message, null);
        }

        private String updateSuppressionPosition(String message, Throwable thrown) {
            String detail = findBlockPosition(message);
            if (!detail.isEmpty()) {
                return detail;
            }

            Throwable current = thrown;
            while (current != null) {
                detail = findBlockPosition(current.getMessage());
                if (!detail.isEmpty()) {
                    return detail;
                }
                current = current.getCause();
            }

            return Messages.unknownPosition();
        }

        private String findBlockPosition(String text) {
            if (text == null || text.isBlank()) {
                return "";
            }
            Matcher positionMatcher = BLOCK_POSITION.matcher(text);
            if (positionMatcher.find()) {
                return positionMatcher.group();
            }
            return "";
        }

        private boolean recentCrashNotice() {
            return System.currentTimeMillis() - lastCrashNoticeMillis < STACK_SUMMARY_SUPPRESSION_MILLIS;
        }

        private boolean hasUpdateSuppressionCause(Throwable throwable) {
            Throwable current = throwable;
            while (current != null) {
                String className = current.getClass().getName().toLowerCase(Locale.ROOT);
                String message = current.getMessage() == null ? "" : current.getMessage().toLowerCase(Locale.ROOT);
                if (className.contains(STACK_OVERFLOW_SUPPRESSION)
                        || className.contains(TIS_UPDATE_SUPPRESSION)
                        || message.contains(UPDATE_NEIGHBORS)) {
                    return true;
                }
                current = current.getCause();
            }
            return false;
        }
    }
}
