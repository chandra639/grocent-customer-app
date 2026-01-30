# Fix "keytool is not recognized" Error

## ⭐ Solution 1: Use Android Studio's keytool (Full Path)

### Step 1: Open Command Prompt
1. Press `Win + R`
2. Type: `cmd`
3. Press Enter

### Step 2: Run This Command (Copy-Paste Entire Line)

**Try this first (most common location):**
```cmd
"C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
```

**If that doesn't work, try this (alternative location):**
```cmd
"C:\Program Files\Android\Android Studio\jre\bin\keytool.exe" -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
```

**Or this (if Android Studio is in different location):**
```cmd
"C:\Users\%USERNAME%\AppData\Local\Android\Sdk\jbr\bin\keytool.exe" -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
```

---

## ⭐ Solution 2: Find keytool Location

### Step 1: Find Android Studio Installation

1. Open **File Explorer**
2. Navigate to: `C:\Program Files\Android\Android Studio\`
3. Look for one of these folders:
   - `jbr\bin\` (JetBrains Runtime - most common)
   - `jre\bin\` (Java Runtime Environment)

### Step 2: Check if keytool.exe Exists

1. Go to: `C:\Program Files\Android\Android Studio\jbr\bin\`
2. Look for `keytool.exe`
3. If found, note the full path

### Step 3: Use the Full Path

Replace `C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe` with your actual path in the command:

```cmd
"YOUR_FULL_PATH_TO_KEYTOOL" -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
```

---

## ⭐ Solution 3: Use Android Studio Terminal (Easiest!)

Android Studio Terminal has the correct PATH set up automatically.

### Step 1: Open Android Studio Terminal

1. Open **Android Studio**
2. Open your **Grocery** project
3. Click **Terminal** tab at the bottom
   - If not visible: **View** → **Tool Windows** → **Terminal**

### Step 2: Run Command

**Copy and paste this:**
```cmd
keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
```

Press **Enter**

**This should work because Android Studio Terminal has Java in PATH!**

---

## ⭐ Solution 4: Search for keytool.exe

### Step 1: Search Your Computer

1. Press `Win + S` (Windows Search)
2. Type: `keytool.exe`
3. Wait for search results
4. Right-click on `keytool.exe` → **Open file location**
5. Note the full path (e.g., `C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe`)

### Step 2: Use the Path

Use the full path you found in the command:

```cmd
"FULL_PATH_TO_KEYTOOL" -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
```

---

## ⭐ Solution 5: Add Java to PATH (Permanent Fix)

### Step 1: Find Java Installation

1. Open File Explorer
2. Navigate to: `C:\Program Files\Android\Android Studio\jbr\bin\`
3. Copy this path: `C:\Program Files\Android\Android Studio\jbr\bin`

### Step 2: Add to PATH

1. Press `Win + R`
2. Type: `sysdm.cpl`
3. Press Enter
4. Click **Advanced** tab
5. Click **Environment Variables**
6. Under **System variables**, find **Path**
7. Click **Edit**
8. Click **New**
9. Paste: `C:\Program Files\Android\Android Studio\jbr\bin`
10. Click **OK** on all dialogs
11. **Close and reopen Command Prompt**

### Step 3: Test

Now try the original command:
```cmd
keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
```

---

## Quick Test: Which Solution Works?

**Try in this order:**

1. ✅ **Solution 3** (Android Studio Terminal) - Usually works immediately
2. ✅ **Solution 1** (Full path with jbr) - Most common location
3. ✅ **Solution 4** (Search for keytool) - Find exact location
4. ✅ **Solution 2** (Manual search) - If others don't work
5. ✅ **Solution 5** (Add to PATH) - Permanent fix

---

## Recommended: Use Android Studio Terminal

**The easiest solution is Solution 3** - just use Android Studio Terminal. It already has Java configured correctly!

1. Open Android Studio
2. Open Terminal tab (bottom)
3. Run the command
4. Done!

---

## What to Do After Getting SHA Values

Once you get the SHA-1 and SHA-256 values:

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your **grocent** project
3. Click **⚙️ Project Settings**
4. Scroll to **"Your apps"** section
5. Find **Grocent** app
6. Click **"Add fingerprint"**
7. Paste **SHA-1** → Save
8. Click **"Add fingerprint"** again
9. Paste **SHA-256** → Save
10. **Re-download** `google-services.json`
11. Replace the file in your project
12. Clean and rebuild

---

## Still Having Issues?

If none of the solutions work:

1. **Tell me which Android Studio version you have**
2. **Check if you can find keytool.exe** using Windows Search
3. **Try Solution 3** (Android Studio Terminal) - it's the most reliable!
