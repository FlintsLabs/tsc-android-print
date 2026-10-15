# แก้ปัญหาการพิมพ์

## Add printer by IP แล้วไม่พบเครื่อง

ติดตั้ง APK และเปิด **TSC Direct Print** ใน Settings การพิมพ์ แล้วเลือก **TSC MH261T** จากเมนู Print ของแอปต้นทาง System / Default Print Service ใช้โปรโตคอลและการค้นหาคนละแบบ

## ตรวจการเชื่อมต่อไม่ผ่าน

ตรวจ IP และกดบันทึกก่อนตรวจ มือถือต้องเข้าถึง TCP 9100 ของเครื่องพิมพ์ได้ แม้อยู่ SSID เดียวกันก็อาจถูกแยกด้วย guest Wi-Fi / client isolation / VLAN / firewall หาก Android ขอสิทธิ์เครือข่ายภายใน ให้เปิดแอปและอนุญาตก่อน ปุ่มตรวจการเชื่อมต่อไม่ส่งฉลาก

ค่าเริ่มต้น 192.168.70.37 เป็นตัวอย่างจากเครื่องพัฒนา ไม่ใช่ IP ที่ใช้ได้กับทุกเครือข่าย

## Print service not enabled / This printer isn't available right now

ตรวจ **Android Settings → Printing → TSC Direct Print → On** ปิดหน้าต่าง Print เดิมและสร้างงานใหม่ หาก Paper size ว่าง หน้าพิมพ์ยังไม่ได้รับความสามารถของเครื่องครบ

ข้อความนี้ไม่พิสูจน์ว่าผู้ใช้ปิดสวิตช์เสมอไป Android ใช้ข้อความ service-unavailable เมื่อบริการไม่อยู่หรือขาดการเชื่อมต่อด้วย ดู [AOSP UserState](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/services/print/java/com/android/server/print/UserState.java)

### Xiaomi / Redmi / HyperOS: พิมพ์ได้เฉพาะตอนเสียบ USB

เคส Redmi 15 5G / Android 16 / HyperOS 3.0 พบระบบแช่แข็งโปรเซส TSC ตอนถอดสายและปลดเมื่อเสียบกลับ แม้สวิตช์บริการยังเปิด แอปพิมพ์ผ่าน Wi-Fi โดยตรง ไม่ได้ส่งงานผ่าน Mac หรือ USB

ขั้นตอนที่ใช้กู้คืน:

1. เข้า **Settings → Apps → Manage apps → TSC Direct Print → Power**
2. เลือก **No restrictions** เฉพาะแอป TSC
3. รีสตาร์ต Android Print Spooler โดยไม่ล้างข้อมูล แล้วตรวจว่า TSC Direct Print ยังเป็น On
4. ปิด Print dialog เดิม สร้างงานใหม่จากเว็บ/แอปต้นทาง และทดสอบขณะถอด USB

No restrictions หยุดการแช่แข็งในเคสนี้ แต่ต้องรีสตาร์ต spooler เพิ่มก่อนสำเร็จ ผู้ใช้ยืนยันผลจริง 1 งานใหม่โดยถอด USB แล้ว ยังไม่ได้ทดสอบข้าม reboot / idle ระยะยาว

สำหรับผู้พัฒนาที่ใช้ ADB และไม่มีงานกำลังส่ง:

```sh
adb -s <device-serial> shell am force-stop --user 0 com.android.printspooler
adb -s <device-serial> shell am start -a android.settings.ACTION_PRINT_SETTINGS
```

คำสั่งนี้ปิดหน้าคิวที่กำลังเปิดอยู่ ไม่ได้ล้างข้อมูล หลังการกู้คืนไม่ต้องใช้ USB debugging เพื่อพิมพ์ ตรวจฉลากที่ออกไปแล้วก่อนสั่งงาน error เก่าซ้ำ

Xiaomi อธิบายการจำกัดแอปผ่าน PowerKeeper ใน [เอกสารผู้พัฒนา](https://dev.mi.com/xiaomihyperos/documentation/detail?pId=1628) ชื่อเมนูและนโยบายต่างกันได้ตามรุ่น

### เก็บ log โดยไม่รบกวนงาน

ใช้ logcat แบบเจาะจง package/process และดูสวิตช์ใน Settings หลีกเลี่ยง global `dumpsys print` ระหว่าง acceptance test บน HyperOS ที่มี dual-app user: dump ของ upstream Android สามารถ lazy-bind spooler และตั้ง callback ได้ จึงไม่ใช่เพียงอ่าน snapshot เสมอไป ดู [RemotePrintSpooler.dump](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/services/print/java/com/android/server/print/RemotePrintSpooler.java)

## พื้นหลัง SHELF / ข้อความสีขาวหาย

ลอง Save as PDF จากต้นทาง หาก PDF ไม่มีพื้นดำ ให้แก้ print CSS / การสร้าง PDF ของต้นทางก่อน แอปพิมพ์กู้ข้อมูลที่ไม่มีใน PDF ไม่ได้ หน้า Print Test มีเลข 4 สีขาวบนพื้นดำสำหรับแยกปัญหานี้

## ขอบหาย / ขนาดไม่ตรง

เลือก Paper size ให้ตรงฉลาก วัดเส้นทดสอบ 80 มม. และตรวจ calibration / Shift / Reference ที่เครื่อง แอปไม่แก้ offset หรือ sensor อัตโนมัติ กรอบครบใน bitmap ไม่ยืนยันการป้อนกระดาษจริง

## งานล้มเหลว/ยกเลิกแล้วมีฉลากออกบางส่วน

เครื่องอาจพิมพ์ข้อมูลที่รับไปแล้ว แอปไม่ retry อัตโนมัติ และการยกเลิกไม่เรียกคืน bytes ที่ส่งไป ตรวจฉลากก่อนสร้างงานซ้ำ
