# TSC Direct Print

**พิมพ์ฉลากจาก Android ตรงถึง TSC MH261T ผ่าน Wi-Fi / LAN ด้วยเมนู Print ทั่วไป**

An Android Print Service that sends PDF labels directly to a TSC MH261T over TCP 9100, with print tests and custom paper sizes. [English guide](docs/README.en.md).

[![Android 13+](https://img.shields.io/badge/Android-13%2B-245B48)](app/src/main/AndroidManifest.xml)
[![MIT License](https://img.shields.io/badge/License-MIT-245B48)](LICENSE)
[![Android build](https://github.com/FlintsLabs/tsc-android-print/actions/workflows/android.yml/badge.svg)](https://github.com/FlintsLabs/tsc-android-print/actions/workflows/android.yml)

**[ดาวน์โหลด APK](https://github.com/FlintsLabs/tsc-android-print/releases/latest)** · [ติดตั้ง](#ติดตั้งและเริ่มใช้งาน) · [แก้ปัญหา](docs/troubleshooting.md) · [Build](docs/building.md) · [Changelog](CHANGELOG.md)

<p align="center">
  <img src="docs/images/print.png" width="250" alt="หน้าพิมพ์ PDF" />
  <img src="docs/images/print-test.png" width="250" alt="หน้า Print Test พร้อมตัวอย่างฉลาก" />
  <img src="docs/images/settings.png" width="250" alt="หน้าตั้งค่า IP เครื่องพิมพ์" />
</p>

ภาพจาก APK 1.2.0 บน Android emulator; IP ในภาพเป็นตัวอย่าง ต้องตั้งให้ตรงกับเครื่องพิมพ์ของคุณ ภาพ system Print ใช้ loopback receiver สำหรับ preview

## ความสามารถ

- ใช้ **Print** จาก browser, โปรแกรมอ่าน PDF และแอปที่รองรับ Android printing
- เลือก PDF จากในแอป แล้วเปิดหน้าพิมพ์ของ Android
- แปลง PDF → ภาพขาวดำ 203 DPI → TSPL `BITMAP` → เครื่องพิมพ์โดยตรง
- Preset **102 × 76 มม.** และ **148 × 210 มม. (A5)** พร้อมเพิ่มขนาดเอง
- **Print Test** มีกรอบ 4 ด้าน เส้นวัด QR และข้อความสีขาวบนพื้นดำ
- เลือกหน้า จำนวนสำเนา และแนวกระดาษจากหน้าพิมพ์ของระบบ
- UI ภาษาไทย ธีมสว่าง/มืด และ navigation rail สำหรับจอกว้าง

ไม่ต้องใช้ Mac, USB หรือ print server เป็นตัวกลาง ไม่มี telemetry และไม่ส่งเอกสารไป cloud

## สิ่งที่ต้องมี

| รายการ | รองรับ |
| --- | --- |
| มือถือ / tablet | Android 13 ขึ้นไป (API 33+) |
| เครื่องพิมพ์ที่ทดสอบจริง | TSC MH261T, 203 DPI |
| เครือข่าย | มือถือเข้าถึง IPv4 ของเครื่องพิมพ์ผ่าน TCP 9100 ได้ |
| กระดาษ | กว้าง 20–168 มม. × สูง 20–500 มม. ภายในข้อจำกัดของแอป |
| เอกสารต้นทาง | PDF หรือแอปที่มีเมนู Android Print |

รุ่น TSPL อื่นและหัวพิมพ์ 300/600 DPI ยังไม่ได้ทดสอบ เวอร์ชันนี้กำหนด 203 DPI และแสดงเครื่องพิมพ์ 1 เครื่อง

## ติดตั้งและเริ่มใช้งาน

1. ดาวน์โหลด `TSC-Direct-Print-1.2.0.apk` จาก **[Releases](https://github.com/FlintsLabs/tsc-android-print/releases/latest)**
2. เปิด APK บน Android 13+ และอนุญาตติดตั้งจากแอปที่เปิดไฟล์เมื่อ Android ขอ
3. เปิด **TSC Direct Print → ตั้งค่า** ใส่ **IP เครื่องพิมพ์** แล้วกด **บันทึก IP** ค่าเริ่มต้นคือ `192.168.70.37`; เปลี่ยนให้ตรงกับเครือข่ายของคุณ
4. กด **ตรวจการเชื่อมต่อ** ปุ่มนี้ตรวจ TCP 9100 โดยยังไม่ส่งฉลาก
5. กด **จัดการบริการพิมพ์** แล้วเปิด **TSC Direct Print** ในหน้าการพิมพ์ของ Android
6. เลือกขนาดให้ตรงกับฉลาก แล้วเปิดเอกสารและกด **Print → TSC MH261T**
7. ตรวจ preview, Paper size, แนวกระดาษ, หน้า และจำนวนสำเนา ก่อนยืนยันพิมพ์

อนุญาตสิทธิ์เครือข่ายภายในเมื่อ Android รุ่นที่ใช้งานขอ ชื่อเมนูระบบอาจต่างกันตามยี่ห้อ

ไม่ต้องเพิ่ม IP ผ่าน **System / Default Print Service → Add printer by IP**: ให้เลือกบริการ **TSC Direct Print** ที่ติดตั้งจาก APK นี้

สำหรับเครื่อง handheld ดู **[ขั้นตอนติดตั้งบน CipherLab RS38](docs/rs38.md)** — ยังต้องทดสอบบริการพิมพ์บนเฟิร์มแวร์ของเครื่องจริง

### พิมพ์จากเว็บไซต์

ใช้ปุ่ม **Print** ของเว็บไซต์หรือเมนู Print ของ browser แล้วเลือก **TSC MH261T** เว็บไซต์/browser ต้องเปิด Android Print dialog ได้ก่อน แอปนี้ไม่ได้เพิ่มความสามารถพิมพ์ให้ WebView ที่ไม่รองรับ

### เลือก PDF จากในแอป

เปิดแท็บ **พิมพ์ → เลือก PDF เพื่อพิมพ์** เลือกไฟล์ แล้วตรวจค่าใน Android Print dialog ก่อนยืนยัน

## ขนาดกระดาษ

เลือก preset **102 × 76 มม.** หรือ **148 × 210 มม.** ได้ทั้งหน้า **พิมพ์**, **Print Test** และ **Paper size** ของ Android

เพิ่มขนาดเองที่ **ตั้งค่า → เพิ่มขนาดกระดาษเอง** ใส่กว้าง/สูงหน่วยมิลลิเมตร แล้วกด **บันทึกและใช้ขนาดนี้** รองรับทศนิยม 1 ตำแหน่งและบันทึกเองได้ 20 รายการ

<p align="center">
  <img src="docs/images/custom-paper.png" width="250" alt="เพิ่มขนาดกระดาษเอง" />
  <img src="docs/images/system-print.png" width="250" alt="หน้าพิมพ์มาตรฐานของ Android" />
</p>

**กว้าง** คือแนวหัวพิมพ์ และ **สูง** คือแนวป้อนกระดาษ ตัวอย่าง A5 ใช้ `148 × 210` แล้วเลือกแนวตั้ง/แนวนอนแยกในหน้าพิมพ์ ช่วง 20–168 × 20–500 มม. เป็นข้อจำกัดของแอป ไม่ใช่ความยาวสูงสุดของเครื่อง

ขนาดใน Print dialog ของงานนั้นเป็นค่าที่ส่งจริง เปลี่ยนม้วนฉลากแล้วต้องตั้งไกด์และ calibrate sensor ที่เครื่องให้ตรงด้วย

## Print Test

1. เปิด **Print Test** และเลือกขนาดกระดาษ
2. กด **พิมพ์ฉลากทดสอบ** เพื่อเปิด Android Print dialog
3. เลือก **TSC MH261T**, ขนาดฉลากที่ใส่จริง และ **1 สำเนา** แล้วกดยืนยัน
4. ตรวจกรอบทั้ง 4 ด้าน เส้นวัด และ QR ซึ่งควรอ่านได้เป็น `TSC-TEST-001`

![ภาพฉลากทดสอบที่ render จากแอป](docs/images/test-label.png)

ภาพนี้เป็นผล render จากแอป ไม่ใช่ภาพถ่ายกระดาษ กรอบห่างขอบประมาณ 2 มม. เส้นวัดยาว 80 มม. หรือสั้นลงพร้อมระบุความยาวเมื่อใช้กระดาษแคบ การเปิด preview ยังไม่ส่งงานไปเครื่อง

## การจัดหน้าและข้อจำกัด

- รักษาสัดส่วน ย่อ/ขยายให้พอดี จัดกึ่งกลาง และหมุนตามแนวกระดาษที่เลือก
- ขาวดำด้วย threshold 128 เพื่อคงขอบ barcode; ไม่มี dithering สำหรับภาพถ่าย
- ไม่เขียนค่า Shift, Reference, gap sensor, ribbon, speed หรือ density ของเครื่อง
- PDF สูงสุด 100 MiB / 100 หน้า; สูงสุด 100 สำเนาและรวมไม่เกิน 1,000 ฉลากต่องาน
- เตรียมทุกหน้าก่อนเชื่อมต่อ TCP; สำเนาเรียงเป็นเอกสาร เช่น `1,2,3,1,2,3`
- เมื่อส่งขาดหรือยกเลิกจะไม่ส่งซ้ำอัตโนมัติ เครื่องอาจพิมพ์ข้อมูลที่รับแล้ว จึงต้องตรวจฉลากก่อนสั่งซ้ำ
- สถานะสำเร็จหมายถึงส่งข้อมูลผ่าน TCP แล้ว ยังไม่ได้ยืนยันกระดาษออก / paper out / ribbon / ฝาเปิด
- TCP 9100 ภายในเครือข่ายไม่มี TLS; ไฟล์งานเก็บใน app-private cache และลบเมื่อจบงาน

พื้นหลังที่หายตั้งแต่ PDF ต้องแก้ที่เว็บไซต์/แอปต้นทาง ส่วนขอบเลื่อนหรือป้อนฉลากคลาดเคลื่อนให้ตรวจ calibration และตำแหน่งพิมพ์ที่เครื่อง

## เมื่อเครื่องพิมพ์ไม่พร้อม

ตรวจ **ตั้งค่า → ตรวจการเชื่อมต่อ** และเปิดบริการ **TSC Direct Print** หากเป็น Xiaomi / Redmi / HyperOS และพิมพ์ได้เฉพาะตอนเสียบ USB ให้ตรวจ **App info → Power → No restrictions**; เคสที่ทดสอบต้องรีสตาร์ต Android Print Spooler เพิ่มเติมก่อนพิมพ์งานใหม่ได้

อ่าน **[Troubleshooting](docs/troubleshooting.md)** และ **[Verification](docs/verification.md)** สำหรับขั้นตอนและขอบเขตผลทดสอบ

## สำหรับผู้พัฒนา

ใช้ **JDK 21**, **Android SDK Platform 37**, **Build Tools 36.0.0** และ Gradle wrapper ที่รวมไว้

```sh
git clone https://github.com/FlintsLabs/tsc-android-print.git
cd tsc-android-print
# ตั้ง JAVA_HOME และ ANDROID_HOME ให้ตรงกับเครื่องก่อน
./gradlew :core:test :app:lintDebug :app:assembleDebug
```

ผล build อยู่ `~/.cache/tsc-android-print/build/` หรือ path ที่กำหนดด้วย `TSC_BUILD_ROOT` ดู test, signing และ release ที่ **[Building](docs/building.md)** และเส้นทาง PDF → TSPL ที่ **[Architecture](docs/architecture.md)**

## License และเครดิต

[MIT License](LICENSE) · FlintsLabs contributors · [Third-party notices](NOTICE)

โครงการอิสระ ไม่ใช่แอปทางการหรือได้รับการรับรองจาก TSC ไม่มี driver binary, firmware หรือ proprietary SDK ของผู้ผลิตรวมอยู่ใน repository หรือ APK
