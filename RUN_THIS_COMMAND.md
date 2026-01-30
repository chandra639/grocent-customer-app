# Run This Command in PowerShell

## Copy and Paste This Entire Line:

Since you're in PowerShell, use this command with the full path:

```powershell
& "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" -list -v -keystore "$env:USERPROFILE\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
```

**Note:** In PowerShell, use `$env:USERPROFILE` instead of `%USERPROFILE%`, and use `&` before the path if it has spaces.

---

## If That Doesn't Work, Try These Alternatives:

### Option 1: Different Android Studio Location
```powershell
& "C:\Program Files\Android\Android Studio\jre\bin\keytool.exe" -list -v -keystore "$env:USERPROFILE\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
```

### Option 2: User-Specific Location
```powershell
& "$env:LOCALAPPDATA\Android\Sdk\jbr\bin\keytool.exe" -list -v -keystore "$env:USERPROFILE\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
```

### Option 3: Find keytool First
1. Press `Win + S`
2. Type: `keytool.exe`
3. Right-click → **Open file location**
4. Copy the full path
5. Use it in the command above

---

## What You'll See:

After running the command, look for these two lines:

```
SHA1: XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX
SHA256: YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY
```

**Copy both values!**

---

## Quick Test:

Try the first command above. If it says "file not found", try Option 1 or Option 2.
