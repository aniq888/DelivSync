# 🔑 How to Get Your Firebase Token - EASY METHOD

## ✅ Your TestApiActivity is Ready!

I've set up everything for you. Here's how to use it:

---

## Step 1: Run Your App

1. **Sync Gradle** in Android Studio (if you haven't already)
2. **Run your app** on emulator or device
3. **Login** to the app with your credentials

---

## Step 2: Open TestApiActivity

### Method 1: Long-Press (Easiest) ⭐

1. Once logged in, you'll see the main screen with bottom navigation
2. **Long-press on the bottom navigation bar** (hold for 1 second)
3. TestApiActivity will open automatically!

### Method 2: Automatic Token in Logcat

When you start the app, the token is **automatically logged** to Logcat:

1. Open **Logcat** in Android Studio
2. Filter by: `FIREBASE_TOKEN`
3. Look for the token (starts with `eyJ...`)
4. Copy the entire token string

---

## Step 3: Get Your Token

Once TestApiActivity opens, you'll see:

1. **Title**: 🔑 API Testing Tools
2. **Token Display**: Shows your Firebase token
3. **Buttons**:
   - 🔄 Refresh Token
   - 🎯 Test Admin API
   - 💰 Test Driver API

### The token is automatically:

✅ **Copied to clipboard** - Just paste it!
✅ **Logged to Logcat** - Filter: `FIREBASE_TOKEN`
✅ **Saved to file** - `firebase_token.txt` in app data

---

## Step 4: Use Token in Postman

1. **Open Postman**
2. **Import collection**: `backend/DelivSync_API.postman_collection.json`
3. **Click** on "DelivSync API" collection
4. **Go to** "Variables" tab
5. **Paste token** in `authToken` field (the token is already in your clipboard!)
6. **Save**
7. **Test endpoints** - All should work now! ✅

---

## Where to Find Your Token

### Option 1: Clipboard (Easiest)
Just **paste** (Ctrl+V) - it's already copied!

### Option 2: Logcat
```
Filter: FIREBASE_TOKEN
Look for the long string starting with: eyJ...
```

### Option 3: File
```
Location: app/files/firebase_token.txt
Access via: Android Studio → Device File Explorer
Path: /data/data/com.example.driverapp/files/firebase_token.txt
```

### Option 4: TestApiActivity Screen
The token is displayed on screen (you can scroll to see it all)

---

## Quick Test Steps

1. **Long-press bottom navigation** in your app
2. TestApiActivity opens
3. Token is **automatically**:
   - ✅ Copied to clipboard
   - ✅ Logged to Logcat
   - ✅ Saved to file
4. **Open Postman**
5. **Paste** token in collection variables
6. **Test APIs** - Done! 🎉

---

## Testing the APIs

### From TestApiActivity:

**Test Admin API:**
- Click "🎯 Test Admin API (Assign Delivery)"
- Creates a test delivery
- Shows delivery ID in results
- Check Firebase Console to verify

**Test Driver API:**
- First create a delivery using Admin API
- Copy the delivery ID
- Click "💰 Test Driver API (Submit COD)"
- Submits COD for that delivery

### From Postman:

1. **Health Check** - No auth needed
2. **Admin → Assign Delivery** - Uses your token
3. **Driver → Submit COD** - Uses your token
4. **Admin → Get COD Submissions** - Uses your token

All endpoints will work because you have a valid token! ✅

---

## What You'll See

### In TestApiActivity:
```
✅ Firebase Token Retrieved!

📋 Token has been:
• Copied to clipboard
• Saved to app files
• Logged to Logcat

📱 User ID: [your-uid]
📧 Email: [your-email]

🔗 Token Preview:
eyJhbGciOiJSUzI1NiIsImtpZCI6Ij...

💡 How to use:
1. Paste in Postman as 'authToken' variable
2. Or check Logcat with filter: FIREBASE_TOKEN
3. Or find file: firebase_token.txt in app data

⏰ Token expires in: 1 hour
```

### In Logcat:
```
D/FIREBASE_TOKEN: ========================================
D/FIREBASE_TOKEN: 🔑 FIREBASE ID TOKEN FOR POSTMAN
D/FIREBASE_TOKEN: ========================================
D/FIREBASE_TOKEN: 
D/FIREBASE_TOKEN: Copy this entire token:
D/FIREBASE_TOKEN: 
D/FIREBASE_TOKEN: eyJhbGciOiJSUzI1NiIsImtpZCI6Ij... [full token]
D/FIREBASE_TOKEN: 
D/FIREBASE_TOKEN: ========================================
```

---

## Troubleshooting

### Can't find TestApiActivity?
1. Make sure Gradle is synced
2. Run the app and login
3. Long-press the bottom navigation bar

### No token appearing?
1. Make sure you're logged in
2. Check Logcat for errors
3. Try clicking "🔄 Refresh Token" button

### Token not working in Postman?
1. Copy the ENTIRE token (very long string)
2. Make sure no extra spaces
3. Token format: `eyJ...` (starts with eyJ)
4. Tokens expire after 1 hour - get a fresh one

---

## Summary

✅ **Long-press bottom navigation** to open TestApiActivity
✅ **Token automatically copied** to clipboard
✅ **Token logged** to Logcat (filter: FIREBASE_TOKEN)
✅ **Token saved** to file (firebase_token.txt)
✅ **Just paste** in Postman and test!

**That's it! Your token is ready to use! 🚀**

---

## Next Steps

1. ✅ Get token (done automatically!)
2. ✅ Paste in Postman
3. ✅ Test all 4 API endpoints
4. ✅ Verify data in Firebase Console
5. ✅ Show your working APIs to instructor!

**You're all set! Happy testing! 🎉**

---

**Last Updated**: December 3, 2024
**Status**: Ready to Use

