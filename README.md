# MindCache

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


Notes are encrypted with a 256-bit master key

The master key is encrypted using a key (password + salt) and stored in the database in the master_key table

The master key is also stored in the Android Keystore

