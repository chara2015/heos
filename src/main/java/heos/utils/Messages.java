package heos.utils;

import heos.Heos;

/**
 * Localized auth-related messages based on config language.
 */
public final class Messages {
    private Messages() {
    }

    public static boolean isEnglish() {
        return Heos.getConfig() != null && "en_us".equalsIgnoreCase(Heos.getConfig().language);
    }

    public static String authPromptLogin() {
        return isEnglish() ? "§ePlease use /login <password> to log in" : "§e请使用 /login <密码> 登录";
    }

    public static String authPromptRegister() {
        return isEnglish() ? "§ePlease use /register <password> <confirmPassword> to register" : "§e请使用 /register <密码> <确认密码> 注册";
    }

    public static String offlineNameHint() {
        return isEnglish()
                ? "§cInvalid session\n\n§eOffline players must retry with a username containing + - .\n§aAllowed symbols: + - ."
                : "§c无效会话\n\n§e离线玩家请在用户名中加入符号后重试\n§a可用符号：+ - .";
    }

    public static String offlineNameLogOnly() {
        return "HEOS_OFFLINE_NAME_RULE";
    }

    public static String invalidOfflineNameLog() {
        return isEnglish()
                ? "§cOffline player used a name outside the allowed rule"
                : "§c有不在规则内的离线玩家名称";
    }

    public static String loginTimeout() {
        return isEnglish()
                ? "§cLogin timed out! Please complete authentication within 60 seconds"
                : "§c登录超时！请在60秒内完成登录";
    }

    public static String premiumWelcome() {
        return isEnglish() ? "§aWelcome back, premium player!" : "§a欢迎回来，正版玩家！";
    }

    public static String loginInputHint() {
        return isEnglish() ? "§ePlease enter: /login <password>" : "§e请输入密码: /login <密码>";
    }

    public static String registerInputHint() {
        return isEnglish()
                ? "§ePlease enter: /register <password> <confirmPassword>"
                : "§e请输入密码: /register <密码> <确认密码>";
    }

    public static String alreadyLoggedIn() {
        return isEnglish() ? "§cYou are already logged in" : "§c你已经登录了";
    }

    public static String premiumNoLogin() {
        return isEnglish() ? "§cPremium players do not need to log in" : "§c正版玩家无需登录";
    }

    public static String premiumNoRegister() {
        return isEnglish() ? "§cPremium players do not need to register" : "§c正版玩家无需注册";
    }

    public static String notRegistered() {
        return isEnglish()
                ? "§cYou are not registered, please use /register <password> <confirmPassword>"
                : "§c你还没有注册，请使用 /register <密码> <确认密码>";
    }

    public static String alreadyRegistered() {
        return isEnglish()
                ? "§cYou are already registered, please use /login <password>"
                : "§c你已经注册过了，请使用 /login <密码>";
    }

    public static String loginSuccess() {
        return isEnglish() ? "§aLogin successful" : "§a登录成功";
    }

    public static String wrongPassword() {
        return isEnglish() ? "§cWrong password" : "§c密码错误";
    }

    public static String passwordTooShort() {
        return isEnglish() ? "§cPassword is too short, minimum 4 characters" : "§c密码太短，至少需要4个字符";
    }

    public static String passwordTooLong() {
        return isEnglish() ? "§cPassword is too long, maximum 32 characters" : "§c密码太长，最多32个字符";
    }

    public static String passwordMismatch() {
        return isEnglish() ? "§cThe two passwords do not match" : "§c两次输入的密码不一致";
    }

    public static String registerFailed() {
        return isEnglish() ? "§cRegistration failed, please contact an admin" : "§c注册失败，请联系管理员";
    }

    public static String registerSuccess() {
        return isEnglish() ? "§aRegistration successful, logged in automatically" : "§a注册成功，已自动登录";
    }

    public static String keepPasswordSafe() {
        return isEnglish() ? "§ePlease keep your password safe" : "§e请妥善保管你的密码";
    }
}
