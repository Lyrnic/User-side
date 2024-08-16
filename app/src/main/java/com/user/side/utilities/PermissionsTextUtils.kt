package com.user.side.utilities

import android.content.Context
import android.graphics.Color
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import androidx.core.text.bold
import com.user.side.R
import java.util.Locale

class PermissionsTextUtils {
    companion object {
        fun getAccessibilityText(context: Context): SpannableStringBuilder {
            val lang = Locale.getDefault().language

            return if (lang == "en") {
                englishAccessibilityText(context)
            } else {
                arabicAccessibilityText(context)
            }
        }

        private fun arabicAccessibilityText(context: Context): SpannableStringBuilder {
            val builder = SpannableStringBuilder()
            builder.append("قم بتفعيل امكانية الوصول لمساعدة التطبيق بالقيام بعملة")
                .append("\n\n\n")
            builder.append("1. قم بالضغط على زر ").bold { append("تمكين") }.append("\n\n")
            builder.append("2. سيتم توجيهك الى اعدادات ").bold { append("امكانية الوصول") }.append("\n\n")
            builder.append("3. أبحث وأضغط على \"").bold { append("التطبيقات التي تم تنزيلها") }.append(" أو ")
                .bold { append("الخدمات المثبتة\" ") }.append("\n\n")
            builder.append("4.ستجد قائمة بالتطبيقات التي تستخدم ").bold { append("امكانية الوصول") }.append("\n\n")
            builder.append("5. أبحث واضغط على ").bold { append(context.getText(R.string.app_name)) }.append("\n\n")
            builder.append("6. قم بتفعيل مفتاح التفعيل").append("\n\n")
            builder.append("7. ارجع للتطبيق مجدداً للأستكمال.")
            return builder
        }

        private fun englishAccessibilityText(context: Context): SpannableStringBuilder {
            val builder = SpannableStringBuilder()
            builder.append("Enable accessibility to help the app perform its function")
                .append("\n\n\n")
            builder.append("1. Tap on the ").bold { append("Enable") }.append(" button").append("\n\n")
            builder.append("2. You will be redirected to ").bold { append("Accessibility settings") }.append("\n\n")
            builder.append("3. Find and tap on ").bold { append("Downloaded apps") }.append(" or ")
                .bold { append("Installed services") }.append("\n\n")
            builder.append("4. A list of apps using ").bold { append("Accessibility Service") }.append(" will appear").append("\n\n")
            builder.append("5. Find and tap on ").bold { append(context.getText(R.string.app_name)) }.append("\n\n")
            builder.append("6. Enable the switch button").append("\n\n")
            builder.append("7. Return to the app to continue.")
            return builder
        }

        // Add the same structure for each permission type
        fun getDrawOverlayPermissionText(context: Context): SpannableStringBuilder {
            val lang = Locale.getDefault().language

            return if (lang == "en") {
                englishDrawOverlayText(context)
            } else {
                arabicDrawOverlayText(context)
            }
        }

        private fun arabicDrawOverlayText(context: Context): SpannableStringBuilder {
            val builder = SpannableStringBuilder()
            builder.append("قم بتفعيل امكانية الظهور فوق التطبيقات لمساعدة التطبيق في العمل بشكل صحيح")
                .append("\n\n\n")
            builder.append("1. قم بالضغط على زر ").bold { append("تمكين") }.append("\n\n")
            builder.append("2.سيتم توجيهك الى اعدادات ").bold { append("الظهور فوق التطبيقات") }.append("\n\n")
            builder.append("3. أبحث وأضغط على ").bold { append(context.getText(R.string.app_name)) }.append("\n\n")
            builder.append("4. قم بتفعيل مفتاح التفعيل").append("\n\n")
            builder.append("5. ارجع للتطبيق مجدداً للأستكمال.")
            return builder
        }

        private fun englishDrawOverlayText(context: Context): SpannableStringBuilder {
            val builder = SpannableStringBuilder()
            builder.append("Enable drawing over apps to allow the app to function properly")
                .append("\n\n\n")
            builder.append("1. Tap on the ").bold { append("Enable") }.append(" button").append("\n\n")
            builder.append("2. You will be redirected to ").bold { append("Draw over other apps settings") }
            builder.append("3. Find and tap on ").bold { append(context.getText(R.string.app_name)) }.append("\n\n")
            builder.append("4. Enable the switch button").append("\n\n")
            builder.append("5. Return to the app to continue.")
            return builder
        }

        fun getNotificationListenerPermissionText(context: Context): SpannableStringBuilder {
            val lang = Locale.getDefault().language

            return if (lang == "en") {
                englishNotificationListenerText(context)
            } else {
                arabicNotificationListenerText(context)
            }
        }

        private fun arabicNotificationListenerText(context: Context): SpannableStringBuilder {
            val builder = SpannableStringBuilder()
            builder.append("قم بتفعيل اذن الوصول الى الاشعارات لمساعدة التطبيق في العمل بشكل صحيح").append("\n\n\n")
            builder.append("1. قم بالضغط على زر ").bold { append("تمكين") }.append("\n\n")
            builder.append("2.سيتم توجيهك الى اعدادات ").bold { append("الوصول الى الاشعارات") }.append("\n\n")
            builder.append("3. ابحث عن ").bold { append(context.getText(R.string.app_name)) }.append("\n\n")
            builder.append("4. قم بتفعيل مفتاح التفعيل ").append("\n\n")
            builder.append("5.ارجع للتطبيق مجدداً للأستكمال.")
            return builder
        }

        private fun englishNotificationListenerText(context: Context): SpannableStringBuilder {
            val builder = SpannableStringBuilder()
            builder.append("Enable notification access to allow the app to function properly").append("\n\n\n")
            builder.append("1. Tap on the ").bold { append("Enable") }.append(" button").append("\n\n")
            builder.append("2. You will be redirected to ").bold { append("Notification Access settings") }.append("\n\n")
            builder.append("3. find ").bold { append(context.getText(R.string.app_name)) }.append("\n\n")
            builder.append("4. Enable the switch button").append("\n\n")
            builder.append("5. Return to the app to continue.")
            return builder
        }

        fun getMiuiDisplayPopUpPermissionText(context: Context): SpannableStringBuilder {
            val lang = Locale.getDefault().language

            return if (lang == "en") {
                englishMiuiDisplayPopUpText(context)
            } else {
                arabicMiuiDisplayPopUpText(context)
            }
        }

        private fun arabicMiuiDisplayPopUpText(context: Context): SpannableStringBuilder {
            val builder = SpannableStringBuilder()
            builder.append("اسمح بفتح نوافذ جديدة أثناء التشغيل في الخلفية لمساعدة التطبيق في العمل بشكل صحيح")
                .append("\n\n\n")
            builder.append("1. قم بالضغط على زر ").bold { append("تمكين") }.append("\n\n")
            builder.append("2. اضغط على ").bold { append("أذونات الاخرى") }.append("\n\n")
            builder.append("3. اضغط على ").bold { append("فتح نوافذ جديدة أثناء التشغيل في الخلفية") }.append("\n\n")
            builder.append("4.  اضغط على ").bold { append("السماح دائما") }.append("\n\n")
            builder.append("5.ارجع للتطبيق مجدداً للأستكمال.")
            return builder
        }

        private fun englishMiuiDisplayPopUpText(context: Context): SpannableStringBuilder {
            val builder = SpannableStringBuilder()
            builder.append("Allow pop-ups while running in the background to help the app function properly")
                .append("\n\n\n")
            builder.append("1. Tap the ").bold { append("Enable") }.append(" button").append("\n\n")
            builder.append("2. Tap on ").bold { append("Other permissions") }.append("\n\n")
            builder.append("3. Tap on ").bold { append("Display pop-ups while running in the background") }.append("\n\n")
            builder.append("4. Tap ").bold { append("Allow always") }.append("\n\n")
            builder.append("5. Return to the app to continue.")
            return builder
        }

        fun getMiuiAutoStartPermissionText(context: Context): SpannableStringBuilder {
            val lang = Locale.getDefault().language

            return if (lang == "en") {
                englishMiuiAutoStartText(context)
            } else {
                arabicMiuiAutoStartText(context)
            }
        }

        private fun arabicMiuiAutoStartText(context: Context): SpannableStringBuilder {
            val builder = SpannableStringBuilder()
            builder.append("قم بتفعيل امكانية التشغيل التلقائي لمساعدة التطبيق في العمل بشكل صحيح")
                .append("\n\n\n")
            builder.append("1. قم بالضغط على زر ").bold { append("تمكين") }.append("\n\n")
            builder.append("2. سيتم توجيهك الى اعدادات ").bold { append("التشغيل التلقائي") }.append("\n\n")
            builder.append("3. ابحث عن ").bold { append(context.getText(R.string.app_name)) }.append("\n\n")
            builder.append("4. قم بتفعيل مفتاح التفعيل ").append("\n\n")
            builder.append("5. ارجع للتطبيق مجدداً للأستكمال.")
            return builder
        }

        private fun englishMiuiAutoStartText(context: Context): SpannableStringBuilder {
            val builder = SpannableStringBuilder()
            builder.append("Enable auto-start to allow the app to function properly")
                .append("\n\n\n")
            builder.append("1. Tap on the ").bold { append("Enable") }.append(" button").append("\n\n")
            builder.append("2. You will be redirected to ").bold { append("Auto-start settings") }.append("\n\n")
            builder.append("3. find ").bold { append(context.getText(R.string.app_name)) }.append("\n\n")
            builder.append("4. Enable the switch button").append("\n\n")
            builder.append("5. Return to the app to continue.")
            return builder
        }

        fun getBatteryOptimizationPermissionText(context: Context): SpannableStringBuilder {
            val lang = Locale.getDefault().language

            return if (lang == "en") {
                englishBatteryOptimizationText(context)
            } else {
                arabicBatteryOptimizationText(context)
            }
        }

        private fun arabicBatteryOptimizationText(context: Context): SpannableStringBuilder {
            val builder = SpannableStringBuilder()
            builder.append("قم بتعطيل تحسين البطارية لمساعدة التطبيق في العمل بشكل صحيح")
                .append("\n\n\n")
            builder.append("1. قم بالضغط على زر ").bold { append("تمكين") }.append("\n\n")
            builder.append("2. سيتم عرض نافذة بها خيارات ").bold { append("تحسين البطارية") }.append(" أو ")
                .append("نافذة تأكيد تطالبك بالسماح بتجاهل تحسينات البطارية").append("\n\n")
            builder.append("3. اضغط على ").bold { append("لا يوجد قيود") }.append(" أو ").bold { append("زر سماح") }.append("\n\n")
            builder.append("4. ارجع للتطبيق مجدداً للأستكمال.")
            return builder
        }

        private fun englishBatteryOptimizationText(context: Context): SpannableStringBuilder {
            val builder = SpannableStringBuilder()
            builder.append("Disable battery optimization to help the app function properly")
                .append("\n\n\n")
            builder.append("1. Tap on the ").bold { append("Enable") }.append(" button")
                .append("\n\n")
            builder.append("2. A window will appear with options for ").bold { append("Battery Optimization") }
                .append(" or a confirmation window asking you to agree to ignore battery optimizations").append("\n\n")
            builder.append("3. Tap on ").bold { append("No restrictions") }.append(" or ").bold { append("Allow button") }.append("\n\n")
            builder.append("4. Return to the app to continue.")
            return builder
        }


        fun getManageStoragePermissionText(context: Context): SpannableStringBuilder {
            val lang = Locale.getDefault().language

            return if (lang == "en") {
                englishManageStorageText(context)
            } else {
                arabicManageStorageText(context)
            }
        }

        private fun arabicManageStorageText(context: Context): SpannableStringBuilder {
            val builder = SpannableStringBuilder()
            builder.append("قم بتفعيل ادارة التخزين لمساعدة التطبيق في العمل بشكل صحيح")
                .append("\n\n\n")
            builder.append("1. قم بالضغط على زر ").bold { append("تمكين") }.append("\n\n")
            builder.append("2.سيتم توجيهك الى اعدادات ").bold { append("ادارة التخزين") }.append("\n\n")
            builder.append("3. قم بتفعيل مفتاح التفعيل").append("\n\n")
            builder.append("4. ارجع للتطبيق مجدداً للأستكمال.")
            return builder
        }

        private fun englishManageStorageText(context: Context): SpannableStringBuilder {
            val builder = SpannableStringBuilder()
            builder.append("Enable storage management to allow the app to function properly").append("\n\n\n")
            builder.append("1. Tap on the ").bold { append("Enable") }.append(" button").append("\n\n")
            builder.append("2. You will be redirected to ").bold { append("Manage Storage settings") }.append("\n\n")
            builder.append("3. ُEnable manage storage switch").append("\n\n")
            builder.append("4. Return to the app to continue.")
            return builder
        }

        fun getRestrictedSettingsText(context: Context): SpannableStringBuilder {
            if(XiaomiUtilities.isMIUI()){
                return getRestrictedSettingsTextForMiui(context)
            }

            val lang = Locale.getDefault().language

            return if (lang == "en") {
                englishRestrictedSettingsText(context)
            } else {
                arabicRestrictedSettingsText(context)
            }
        }

        private fun arabicRestrictedSettingsText(context: Context): SpannableStringBuilder {
            val builder = SpannableStringBuilder()
            builder.append("قم بتفعيل الإعدادات المقيدة لمساعدة التطبيق في العمل بشكل صحيح")
                .append("\n\n\n")
            builder.append("1. قم بالضغط على زر ").bold { append("تمكين") }.append("\n\n")
            builder.append("2. سيتم توجيهك إلى ").bold { append("إعدادات معلومات التطبيق") }.append("\n\n")
            builder.append("3. اضغط على النقاط الثلاث في أعلى الشاشة ").bold { append("ثم اختر") }
                .append(" ").bold { append("السماح بالإعدادات المقيدة") }.append("\n\n")

            val warningText = "4. إذا لم يظهر خيار " +
                    "السماح بالإعدادات المقيدة" +
                    "، قم بالتنقل إلى الصفحة التالية: " +
                    "امكانية الوصول" +
                    "، وحاول تفعيل امكانية الوصول بشكل غير ناجح، ثم عد إلى هذه الإعدادات مجددا لمحاولة تفعيل الإعدادات المقيدة"

            builder.append(warningText)
            applyColorSpan(builder, warningText, Color.RED)

            builder.append("\n\n")
            builder.append("5. ارجع للتطبيق مجدداً للأستكمال.")
            return builder
        }

        private fun englishRestrictedSettingsText(context: Context): SpannableStringBuilder {
            val builder = SpannableStringBuilder()
            builder.append("Enable restricted settings to help the app function properly")
                .append("\n\n\n")
            builder.append("1. Tap on the ").bold { append("Enable") }.append(" button").append("\n\n")
            builder.append("2. You will be redirected to ").bold { append("App Info settings") }.append("\n\n")
            builder.append("3. Tap on the three dots at the top-right corner of the screen ")
                .bold { append("and select") }.append(" ").bold { append("Allow restricted settings") }.append("\n\n")

            val warningText = "4. If the option " +
                    "Allow restricted settings" +
                    " does not appear, navigate to the next tab: " +
                    "accessibility service" +
                    " and attempt to enable it unsuccessfully. Then return to these settings and try enabling restricted settings again"

            builder.append(warningText)
            applyColorSpan(builder, warningText, Color.RED)

            builder.append("\n\n")
            builder.append("5. Return to the app to continue.")
            return builder
        }

        private fun applyColorSpan(builder: SpannableStringBuilder, text: String, color: Int) {
            val start = builder.indexOf(text)
            val end = start + text.length
            if (start >= 0) {
                builder.setSpan(ForegroundColorSpan(color), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }


        fun getRestrictedSettingsTextForMiui(context: Context): SpannableStringBuilder {
            val lang = Locale.getDefault().language

            return if (lang == "en") {
                englishRestrictedSettingsTextForMiui(context)
            } else {
                arabicRestrictedSettingsTextForMiui(context)
            }
        }

        private fun arabicRestrictedSettingsTextForMiui(context: Context): SpannableStringBuilder {
            val builder = SpannableStringBuilder()
            builder.append("قم بتفعيل الإعدادات المقيدة لمساعدة التطبيق في العمل بشكل صحيح")
                .append("\n\n\n")
            builder.append("1. قم بالضغط على زر ").bold { append("تمكين") }.append("\n\n")
            builder.append("2. سيتم توجيهك إلى ").bold { append("إعدادات معلومات التطبيق") }.append("\n\n")
            builder.append("3. قم بالتمرير إلى أسفل القائمة حتى النهاية").append("\n\n")
            builder.append("4. انتظر لمدة 5 ثوانٍ حتى تظهر المزيد من الخيارات").append("\n\n")
            builder.append("5. بعد ظهور الخيارات الجديدة، اضغط على ").bold { append("السماح بالإعدادات المقيدة") }.append("\n\n")
            builder.append("6. ارجع للتطبيق مجدداً للأستكمال.")
            return builder
        }

        private fun englishRestrictedSettingsTextForMiui(context: Context): SpannableStringBuilder {
            val builder = SpannableStringBuilder()
            builder.append("Enable restricted settings to help the app function properly")
                .append("\n\n\n")
            builder.append("1. Tap on the ").bold { append("Enable") }.append(" button").append("\n\n")
            builder.append("2. You will be redirected to ").bold { append("App Info settings") }.append("\n\n")
            builder.append("3. Scroll down to the bottom of the list").append("\n\n")
            builder.append("4. Wait for 5 seconds until more options appear").append("\n\n")
            builder.append("5. Once the new options appear, tap on ").bold { append("Allow restricted settings") }.append("\n\n")
            builder.append("6. Return to the app to continue.")
            return builder
        }


    }
}
