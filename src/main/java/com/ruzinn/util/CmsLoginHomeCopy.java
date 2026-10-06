package com.ruzinn.util;

import com.ruzinn.enums.AppLocale;
import java.util.LinkedHashMap;
import java.util.Map;

public final class CmsLoginHomeCopy {

  private CmsLoginHomeCopy() {}

  public static Map<String, String> page(AppLocale locale) {
    return switch (locale) {
      case HI -> pageHi();
      case RU -> pageRu();
      case AR -> pageAr();
      default -> Map.of();
    };
  }

  public static Map<String, String> slide(int sortOrder, AppLocale locale) {
    return switch (locale) {
      case HI -> slideHi(sortOrder);
      case RU -> slideRu(sortOrder);
      case AR -> slideAr(sortOrder);
      default -> Map.of();
    };
  }

  public static Map<String, String> feature(String iconKey, AppLocale locale) {
    return switch (locale) {
      case HI -> featureHi(iconKey);
      case RU -> featureRu(iconKey);
      case AR -> featureAr(iconKey);
      default -> Map.of();
    };
  }

  private static Map<String, String> pageHi() {
    Map<String, String> map = new LinkedHashMap<>();
    map.put("product_name", "GROUPBOOKINGLA");
    map.put("product_pill", "B2B कंपनी ग्रुप बुकिंग");
    map.put("panel_tagline", "साथ यात्रा करें  •  बेहतर व्यापार  •  अधिक संभावनाएँ");
    map.put("sign_in_title", "साइन इन");
    map.put("sign_in_subtitle", "कृपया साइन इन करने के लिए अपना विवरण दर्ज करें!");
    map.put("register_label", "पंजीकरण");
    map.put("footer_copy", "© 2026 Rathin. सर्वाधिकार सुरक्षित।");
    return map;
  }

  private static Map<String, String> pageRu() {
    Map<String, String> map = new LinkedHashMap<>();
    map.put("product_name", "GROUPBOOKINGLA");
    map.put("product_pill", "B2B групповое бронирование");
    map.put("panel_tagline", "Путешествуйте вместе  •  Лучший бизнес  •  Больше возможностей");
    map.put("sign_in_title", "Вход");
    map.put("sign_in_subtitle", "Введите данные, чтобы войти!");
    map.put("register_label", "Регистрация");
    map.put("footer_copy", "© 2026 Rathin. Все права защищены.");
    return map;
  }

  private static Map<String, String> pageAr() {
    Map<String, String> map = new LinkedHashMap<>();
    map.put("product_name", "GROUPBOOKINGLA");
    map.put("product_pill", "حجوزات المجموعات للشركات");
    map.put("panel_tagline", "سافروا معاً  •  أعمال أفضل  •  إمكانيات أكبر");
    map.put("sign_in_title", "تسجيل الدخول");
    map.put("sign_in_subtitle", "يرجى إدخال بياناتك لتسجيل الدخول!");
    map.put("register_label", "تسجيل");
    map.put("footer_copy", "© 2026 Rathin. جميع الحقوق محفوظة.");
    return map;
  }

  private static Map<String, String> slideHi(int order) {
    return switch (order) {
      case 1 -> slide(
          "ग्रुप बुकिंग अब आसान",
          "बड़ी बचत बड़ी यात्राओं के लिए",
          "बड़ी यात्राओं",
          "साथ में और यात्रा करें",
          "तक",
          "30% छूट",
          "ग्रुप बुकिंग",
          "दुनिया भर में फ्लाइट, होटल और गतिविधियों पर विशेष ग्रुप किराया।",
          "ग्रुप डील्स बुक करें");
      case 2 -> slide(
          "हर ग्रुप के लिए होटल",
          "साथ रहें, कम खर्च करें",
          "कम खर्च करें",
          "साथ बेहतर",
          "तक",
          "25% छूट",
          "ग्रुप ठहराव",
          "शहर और रिसॉर्ट होटलों में विशेष ग्रुप दरों पर कमरे ब्लॉक करें।",
          "ग्रुप डील्स बुक करें");
      case 3 -> slide(
          "आकर्षण और टूर",
          "ग्रुप में और देखें",
          "ग्रुप में",
          "साथ में और यात्रा करें",
          "तक",
          "20% छूट",
          "ग्रुप टिकट",
          "पार्क, सिटी टूर और स्थानीय आकर्षण समर्पित ग्रुप स्लॉट के साथ।",
          "ग्रुप डील्स बुक करें");
      case 4 -> slide(
          "ग्रुप फ्लाइट किराया",
          "साथ उड़ें और बचाएँ",
          "और बचाएँ",
          "अधिक संभावनाएँ",
          "तक",
          "15% छूट",
          "ग्रुप फ्लाइट",
          "समन्वित ग्रुप हवाई किराया ताकि हर यात्री एक ही योजना पर यात्रा करे।",
          "ग्रुप डील्स बुक करें");
      default -> Map.of();
    };
  }

  private static Map<String, String> slideRu(int order) {
    return switch (order) {
      case 1 -> slide(
          "ГРУППОВЫЕ БРОНИРОВАНИЯ ПРОСТО",
          "Большая экономия для больших поездок",
          "для больших",
          "Путешествуйте вместе",
          "ДО",
          "СКИДКА 30%",
          "ГРУППЫ",
          "Специальные групповые тарифы на перелёты, отели и развлечения по всему миру.",
          "Забронировать группу");
      case 2 -> slide(
          "ОТЕЛИ ДЛЯ ЛЮБОЙ ГРУППЫ",
          "Живите вместе и экономьте",
          "и экономьте",
          "Лучше вместе",
          "ДО",
          "СКИДКА 25%",
          "ПРОЖИВАНИЕ",
          "Блокируйте номера по специальным групповым тарифам в городских и курортных отелях.",
          "Забронировать группу");
      case 3 -> slide(
          "ДОСТОПРИМЕЧАТЕЛЬНОСТИ И ТУРЫ",
          "Смотрите больше группой",
          "группой",
          "Путешествуйте вместе",
          "ДО",
          "СКИДКА 20%",
          "БИЛЕТЫ",
          "Парки, городские туры и местные аттракционы с выделенными слотами для групп.",
          "Забронировать группу");
      case 4 -> slide(
          "ГРУППОВЫЕ АВИАТАРЫ",
          "Летите вместе и экономьте",
          "и экономьте",
          "Больше возможностей",
          "ДО",
          "СКИДКА 15%",
          "РЕЙСЫ",
          "Согласованные групповые авиатарифы, чтобы все пассажиры летели по одному плану.",
          "Забронировать группу");
      default -> Map.of();
    };
  }

  private static Map<String, String> slideAr(int order) {
    return switch (order) {
      case 1 -> slide(
          "حجوزات المجموعات بسهولة",
          "توفير أكبر للرحلات الأكبر",
          "للرحلات الأكبر",
          "سافروا معاً أكثر",
          "حتى",
          "خصم 30%",
          "حجوزات المجموعات",
          "أسعار مجموعات حصرية على الرحلات والفنادق والأنشطة حول العالم.",
          "احجز عروض المجموعات");
      case 2 -> slide(
          "فنادق لكل مجموعة",
          "أقيموا معاً بتكلفة أقل",
          "بتكلفة أقل",
          "معاً أفضل",
          "حتى",
          "خصم 25%",
          "إقامات المجموعات",
          "احجزوا غرفاً بأسعار مجموعات خاصة في فنادق المدن والمنتجعات.",
          "احجز عروض المجموعات");
      case 3 -> slide(
          "معالم وجولات",
          "شاهدوا أكثر كمجموعة",
          "كمجموعة",
          "سافروا معاً أكثر",
          "حتى",
          "خصم 20%",
          "تذاكر المجموعات",
          "منتزهات وجولات مدن ومعالم محلية مع فترات مخصصة للمجموعات.",
          "احجز عروض المجموعات");
      case 4 -> slide(
          "أسعار رحلات المجموعات",
          "سافروا معاً ووفّروا",
          "ووفّروا",
          "إمكانيات أكبر",
          "حتى",
          "خصم 15%",
          "رحلات جماعية",
          "أسعار طيران منسّقة للمجموعات حتى يسافر كل راكب على الخطة نفسها.",
          "احجز عروض المجموعات");
      default -> Map.of();
    };
  }

  private static Map<String, String> featureHi(String icon) {
    return switch (icon == null ? "" : icon) {
      case "flights" -> feature("ग्रुप फ्लाइट", "बेहतर किराया");
      case "hotels" -> feature("ग्रुप होटल", "विशेष दरें");
      case "attractions" -> feature("स्थानीय आकर्षण", "ग्रुप छूट");
      case "support" -> feature("समर्पित सहायता", "आपके व्यवसाय के लिए");
      default -> Map.of();
    };
  }

  private static Map<String, String> featureRu(String icon) {
    return switch (icon == null ? "" : icon) {
      case "flights" -> feature("Групповые рейсы", "Лучшие тарифы");
      case "hotels" -> feature("Групповые отели", "Специальные цены");
      case "attractions" -> feature("Местные аттракционы", "Скидки для групп");
      case "support" -> feature("Выделенная поддержка", "Для вашего бизнеса");
      default -> Map.of();
    };
  }

  private static Map<String, String> featureAr(String icon) {
    return switch (icon == null ? "" : icon) {
      case "flights" -> feature("رحلات جماعية", "أسعار أفضل");
      case "hotels" -> feature("فنادق المجموعات", "أسعار خاصة");
      case "attractions" -> feature("معالم محلية", "خصومات المجموعات");
      case "support" -> feature("دعم مخصص", "لأعمالك");
      default -> Map.of();
    };
  }

  private static Map<String, String> slide(
      String eyebrow,
      String title,
      String accent,
      String script,
      String kicker,
      String value,
      String sub,
      String body,
      String cta) {
    Map<String, String> map = new LinkedHashMap<>();
    map.put("eyebrow", eyebrow);
    map.put("title", title);
    map.put("accent_phrase", accent);
    map.put("script_text", script);
    map.put("badge_kicker", kicker);
    map.put("badge_value", value);
    map.put("badge_sub", sub);
    map.put("body", body);
    map.put("cta_label", cta);
    return map;
  }

  private static Map<String, String> feature(String title, String subtitle) {
    Map<String, String> map = new LinkedHashMap<>();
    map.put("title", title);
    map.put("subtitle", subtitle);
    return map;
  }
}
