# Musiqay 1.4.5 — Android Auto + Platforms

## Android Auto
- تحويل خدمة التشغيل من MediaSessionService إلى MediaLibraryService.
- إضافة MediaLibrarySession مخصص للتصفح من Android Auto.
- دعم أقسام السيارة: مضاف حديثًا، المفضلة، المجلدات، قوائم التشغيل، الراديو، وكل الملفات.
- دعم البحث من واجهة السيارة والبحث الصوتي عبر MediaBrowser.
- استخدام نفس منطق بحث السور: اسم السورة يطابق اسم الملف أو رقم السورة الواضح فقط.
- دعم تشغيل الملفات والمحطات مباشرة من واجهة Android Auto.
- إضافة توافق MediaBrowserService القديم إلى جانب Media3.
- إضافة automotive_app_desc.xml وتعريف media capability.
- إبقاء واجهة السيارة Driver-safe؛ Android Auto هو الذي يرسم الواجهة وليس Compose الخاص بالهاتف.

## مركز المنصات
- إضافة شاشة جديدة باسم "المنصات".
- دعم Spotify وYouTube Music وAnghami وSoundCloud وApple Music وDeezer.
- كشف ما إذا كان التطبيق الرسمي مثبتًا وعرض حالة "مثبت" أو "ويب".
- حقل بحث موحد: اكتب الأغنية/الفنان مرة واحدة ثم افتح النتيجة في المنصة المختارة.
- Spotify يستخدم native search URI عند توفر التطبيق.
- باقي الخدمات تستخدم التطبيق الرسمي عند توفره، وإلا يتم فتح صفحة الويب الرسمية.
- لا يتم نسخ أو إعادة بث المحتوى المحمي داخل Musiqay.

## الواجهة
- إضافة اختصار "المنصات" إلى الشاشة الرئيسية بدون زيادة ازدحام Bottom Navigation.
- شاشة المنصات تتبع نفس Premium UI والثيمات الحالية.
- Mini Player يستمر في العمل أثناء التنقل داخل شاشة المنصات.

## الإصدار
- versionName: 1.4.5
- versionCode: 9
- الفرع: develop/v1.4.5-auto-platforms
- لم يتم تشغيل Build بعد.
