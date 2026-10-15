# ภาพหน้าจอประกอบคู่มือ

ภาพหน้าจอ UI ชุดปัจจุบันจับวันที่ **15 ต.ค. 2026** จาก **APK Release 1.3.4 (versionCode 9)** บน Android 16 / API 36 Emulator ธีมสว่าง ภาษาไทย ขนาด 1080 × 2400 พิกเซล / 420 dpi / font scale 1.0 บางภาพเลื่อนหน้าเพื่อให้เห็นปุ่มหรือ preview ที่อธิบาย ไม่มีการแก้หน้าตาหรือข้อความในภาพหลังจับหน้าจอ

APK ต้นทาง: [GitHub Release v1.3.4](https://github.com/FlintsLabs/tsc-android-print/releases/tag/v1.3.4), source `419a5c01ac726ca6b8e0954110062c2a568ebd1a`, SHA-256 `8de3fa6ab9f2af9818bf51112021671ca38e718f65f7651daf94e5951ed8f4b5`

| ภาพ | สิ่งที่แสดง |
| --- | --- |
| [print.png](print.png) | หน้า Print เลือกเครื่องและกระดาษก่อนเลือก PDF |
| [print-test.png](print-test.png) | หน้า Print Test และตัวอย่างฉลาก |
| [printer-selector.png](printer-selector.png) | เมนูเลือกเครื่องพิมพ์ พร้อม IP และกระดาษเริ่มต้นของแต่ละเครื่อง |
| [settings.png](settings.png) | รายการเครื่องพิมพ์ 2 เครื่อง แตะเพื่อเปิดตั้งค่ารายเครื่อง |
| [settings-detail.png](settings-detail.png) | ชื่อ IP พอร์ต และกระดาษเริ่มต้นของเครื่องที่กำลังดู |
| [custom-paper.png](custom-paper.png) | กรอกกระดาษตัวอย่าง 100.5 × 150.2 มม.; ยังไม่ได้กดบันทึก |
| [app-update.png](app-update.png) | ตรวจ GitHub Latest หลังเผยแพร่ 1.3.4 แล้ว เวอร์ชันเท่ากันจึงไม่เสนอให้ดาวน์โหลดซ้ำ |
| [system-print.png](system-print.png) | Preview ใน Android Print dialog ก่อนกดยืนยันพิมพ์ |

โปรไฟล์ **Packing-1** (`127.0.0.1:9100`, 102 × 76 มม.) และ **Packing-2** (`127.0.0.2:9100`, 148 × 210 มม.) เป็นข้อมูลสาธิตบน Emulator ไม่ใช่ IP สำหรับตั้งค่าที่หน้างาน การจับ preview ของระบบใช้ตัวรับจำลองผ่าน ADB reverse และไม่กดยืนยันพิมพ์ ไม่มีการส่งฉลากไปเครื่องพิมพ์จริง ภาพ UI/preview ไม่ใช่หลักฐานการพิมพ์ลงกระดาษ

[test-label.png](test-label.png) เป็นภาพฉลากที่ render สำหรับคู่มือเดิม เก็บไว้ประกอบคำอธิบายกรอบ/QR/ข้อความสีขาว ไม่ใช่ภาพถ่ายฉลากจริง และไม่ได้จับใหม่ในรอบนี้

หากจับภาพใหม่ ให้ใช้ Emulator แยกจากเครื่องที่ใช้งานจริง บันทึกเวอร์ชันและขนาดจอ คงชื่อไฟล์เดิมเมื่อหน้าที่เดิม และตรวจทั้ง PNG กับข้อความใน UI tree ก่อน commit
