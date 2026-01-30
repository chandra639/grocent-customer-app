# How to Get SHA-1 and SHA-256 Fingerprints

## Method 1: Using keytool Command (Easiest - Works Every Time)

### Step 1: Find Your Debug Keystore Location

The debug keystore is usually located at:
```
C:\Users\<YourUsername>\.android\debug.keystore
```

**To find your username:**
- Press `Win + R`
- Type: `%USERPROFILE%`
- Press Enter
- Look for `.android` folder
- Inside it, you'll find `debug.keystore`

### Step 2: Open Command Prompt or PowerShell

1. Press `Win + R`
2. Type: `cmd` or `powershell`
3. Press Enter

### Step 3: Run keytool Command

**For SHA-1:**
```cmd
keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
```

**For SHA-256 only (faster):**
```cmd
keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android | findstr "SHA1 SHA256"
```

### Step 4: Copy the Values

You'll see output like:
```
Alias name: androiddebugkey
Creation date: ...
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: ...
Issuer: ...
Serial number: ...
Valid from: ... until: ...
Certificate fingerprints:
     SHA1: XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX
     SHA256: YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY
```

**Copy both SHA1 and SHA256 values!**

---

## Method 2: Using Android Studio Terminal

### Step 1: Open Terminal in Android Studio

1. Open Android Studio
2. Open your **Grocery** project
3. Click **Terminal** tab at the bottom (or View → Tool Windows → Terminal)

### Step 2: Run keytool Command

**For Windows (PowerShell):**
```powershell
keytool -list -v -keystore "$env:USERPROFILE\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
```

**For Windows (CMD):**
```cmd
keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
```

### Step 3: Copy SHA-1 and SHA-256 Values

Look for the lines starting with `SHA1:` and `SHA256:` and copy those values.

---

## Method 3: Finding signingReport Task in Android Studio

### Option A: Search for Task

1. Open Android Studio
2. Click **Gradle** tab on the right sidebar (if not visible: View → Tool Windows → Gradle)
3. Click the **search/filter icon** (🔍) at the top of Gradle panel
4. Type: `signingReport`
5. Double-click the task that appears

### Option B: Navigate Manually

1. Open Android Studio
2. Click **Gradle** tab on the right sidebar
3. Expand: `Grocery` (root project)
4. Expand: `app`
5. Expand: `Tasks`
6. Expand: `android` (or `verification` or `signing`)
7. Look for `signingReport` task
8. **Double-click** it

### Option C: Use Gradle Tool Window

1. Open Android Studio
2. Go to **View** → **Tool Windows** → **Gradle**
3. In the Gradle tool window, expand your project
4. Look for `signingReport` under any of these:
   - `Tasks` → `android` → `signingReport`
   - `Tasks` → `verification` → `signingReport`
   - `Tasks` → `signing` → `signingReport`

### Option D: Run from Terminal in Android Studio

1. Open **Terminal** in Android Studio (bottom tab)
2. Run:
   ```cmd
   .\gradlew signingReport
   ```
3. Check the **Build** output at the bottom for SHA values

---

## Method 4: Using Gradle Command Line

### Step 1: Open Command Prompt

1. Press `Win + R`
2. Type: `cmd`
3. Press Enter

### Step 2: Navigate to Project

```cmd
cd C:\chandra\App_Design\Grocery
```

### Step 3: Run Gradle Task

```cmd
gradlew signingReport
```

### Step 4: Find SHA Values in Output

Look for lines like:
```
Variant: debug
Config: debug
Store: C:\Users\...\.android\debug.keystore
Alias: AndroidDebugKey
SHA1: XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX
SHA256: YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY
```

---

## Quick Copy-Paste Commands

### For Command Prompt (CMD):
```cmd
keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
```

### For PowerShell:
```powershell
keytool -list -v -keystore "$env:USERPROFILE\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
```

### For Android Studio Terminal:
```cmd
keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
```

---

## What to Look For

After running any command, you need to find these two lines:

```
SHA1: XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX
SHA256: YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY
```

**Copy both values** - you'll need them for Firebase Console!

---

## Troubleshooting

### Error: "keytool is not recognized"

**Solution:** Java is not in your PATH. Try:

1. Find Java installation:
   - Usually at: `C:\Program Files\Java\jdk-XX\bin\keytool.exe`
   - Or: `C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe`

2. Use full path:
   ```cmd
   "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
   ```

### Error: "Keystore file does not exist"

**Solution:** The debug keystore hasn't been created yet. Build your app once in Android Studio, and it will create the keystore automatically.

### Can't Find signingReport Task

**Solution:** Use Method 1 (keytool command) - it's the most reliable method and works every time!

---

## Next Steps

After you get SHA-1 and SHA-256:

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your **grocent** project
3. **Project Settings** → **Your apps** → Find **Grocent** app
4. Click **"Add fingerprint"**
5. Paste SHA-1 → Save
6. Click **"Add fingerprint"** again
7. Paste SHA-256 → Save
8. **Re-download** `google-services.json`
9. Replace the file in your project
10. Clean and rebuild

---

## Recommended Method

**Use Method 1 (keytool command)** - it's the fastest and most reliable way to get SHA fingerprints!
