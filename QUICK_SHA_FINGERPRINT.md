# Quick Guide: Get SHA Fingerprints (No Gradle Tab Needed)

## ⭐ EASIEST METHOD: Use Command Prompt

### Step 1: Open Command Prompt
1. Press `Win + R` (Windows key + R)
2. Type: `cmd`
3. Press Enter

### Step 2: Copy and Paste This Command

**Copy this entire line and paste it in Command Prompt:**

```cmd
keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
```

Press **Enter**

### Step 3: Find SHA Values

Look for these two lines in the output:

```
SHA1: XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX
SHA256: YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY
```

**Copy both values!**

---

## If "keytool is not recognized" Error

### Option 1: Use Android Studio's keytool

1. Open Command Prompt
2. Run this command (replace with your Android Studio path if different):

```cmd
"C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
```

### Option 2: Find keytool Location

1. Open File Explorer
2. Navigate to: `C:\Program Files\Android\Android Studio\jbr\bin\`
3. Look for `keytool.exe`
4. If found, use the full path in the command above
5. If not found, try: `C:\Program Files\Java\jdk-XX\bin\keytool.exe`

---

## Alternative: Use Android Studio Terminal

### Step 1: Open Terminal in Android Studio

1. Open Android Studio
2. Open your **Grocery** project
3. At the bottom, click **Terminal** tab
   - If not visible: **View** → **Tool Windows** → **Terminal**

### Step 2: Run Command

**Copy and paste this:**

```cmd
keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
```

Press **Enter**

### Step 3: Copy SHA Values

Find and copy the SHA1 and SHA256 values from the output.

---

## Show Gradle Tab in Android Studio (Optional)

If you want to use Gradle tab later:

1. **View** → **Tool Windows** → **Gradle**
   - OR: Click **Gradle** icon on the right sidebar
   - OR: Press `Alt + 4` (Windows)

2. If still not visible:
   - **File** → **Settings** (or **Preferences** on Mac)
   - **Appearance & Behavior** → **Tool Windows**
   - Make sure **Gradle** is enabled

---

## What to Do After Getting SHA Values

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your **grocent** project
3. Click **⚙️ Project Settings** (gear icon)
4. Scroll to **"Your apps"** section
5. Find **Grocent** app (`com.codewithchandra.grocent`)
6. Click **"Add fingerprint"**
7. Paste **SHA-1** → Click **Save**
8. Click **"Add fingerprint"** again
9. Paste **SHA-256** → Click **Save**
10. **Re-download** `google-services.json`
11. Replace the file in your project
12. Clean and rebuild

---

## Quick Copy-Paste Commands

### For Command Prompt:
```cmd
keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
```

### If keytool not found, use full path:
```cmd
"C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
```

---

## Troubleshooting

### Error: "Keystore file does not exist"

**Solution:** Build your app once in Android Studio. This will create the debug keystore automatically.

1. Open Android Studio
2. Open your Grocery project
3. Click **Build** → **Make Project** (or press `Ctrl + F9`)
4. Wait for build to complete
5. Then try the keytool command again

---

## Summary

**You don't need the Gradle tab!** Just use Command Prompt with the keytool command - it's the fastest and most reliable method.

**Recommended:** Use Command Prompt method (first method above) - it works every time!
