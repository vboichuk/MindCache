# MindCache Android

MindCache - Secure Notes Application

## Overview

**MindCache** is an Android application for personal journaling and note-taking with a focus on security and privacy. All notes are encrypted, and access is protected by password and biometric authentication.

**Key Features**
- Security: AES-256 encryption for all notes
- Authentication: Password-based login with biometric support (Face ID / Fingerprint)
- Note Management: Create, edit, and delete notes
- Local Storage: All data stored exclusively on device
- Auto-save: Draft persistence across navigation and configuration changes

**Tech Stack**
- Language: Java
- Architecture: MVVM
- Database: Room
- Security: Android Keystore, AES-GCM encryption, PBKDF2 key derivation
- Async Operations: RxJava
- UI: Material Design components, View Binding

**Security Architecture**

* Notes are encrypted using AES-256 with a unique IV (Initialization Vector) per note.
* Master Key (256-bit) 
	– is used for encrypting all notes
    - is stored in secure Android Keystore
    - the encrypted master key is stored in the master_key table in the database
	- is never stored as plaintext

* User password
	- is used for encrypting the master key before storing
	- is never stored anywhere

* PBKDF2 with 600,000 iterations is used to derive a key from the password and salt.
* Biometric authentication is supported for unlocking.


**Requirements**
- Android 8.0+ (API level 26+)
- Biometric sensor (optional, for biometric authentication)

