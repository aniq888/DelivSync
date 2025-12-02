# Firebase Setup Instructions

## Critical: Enable Email/Password Authentication

The error `CONFIGURATION_NOT_FOUND` means Email/Password authentication is not enabled in your Firebase project.

### Steps to Fix:

1. **Go to Firebase Console**
   - Visit: https://console.firebase.google.com/
   - Select your project: `driverapp888`

2. **Enable Authentication**
   - Click on **Authentication** in the left sidebar
   - Click on **Get Started** (if first time)
   - Go to the **Sign-in method** tab

3. **Enable Email/Password**
   - Click on **Email/Password**
   - Toggle **Enable** to ON
   - Click **Save**

4. **Optional: Enable Email Link (Passwordless)**
   - You can also enable "Email link (passwordless sign-in)" if needed
   - For now, just enable the basic Email/Password provider

5. **Test Again**
   - After enabling, wait 1-2 minutes for changes to propagate
   - Try signing up again in the app

## Additional Firebase Setup

### Firestore Database
1. Go to **Firestore Database** in Firebase Console
2. Click **Create database**
3. Choose **Start in test mode** (for development)
4. Select your preferred location
5. Click **Enable**

### Firestore Security Rules
Update your Firestore rules in the **Rules** tab:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /drivers/{driverId} {
      allow read, write: if request.auth != null && request.auth.uid == driverId;
    }
    match /deliveries/{deliveryId} {
      allow read, write: if request.auth != null;
    }
    match /cod_submissions/{codId} {
      allow read, write: if request.auth != null;
    }
    match /performance/{driverId} {
      allow read, write: if request.auth != null && request.auth.uid == driverId;
    }
    match /images/{imageId} {
      allow read, write: if request.auth != null;
    }
  }
}
```

### Firebase Cloud Messaging (FCM)
1. Go to **Cloud Messaging** in Firebase Console
2. No additional setup needed for basic push notifications
3. The app will automatically register for FCM tokens

## Verification Checklist

- [ ] Email/Password authentication enabled
- [ ] Firestore Database created
- [ ] Firestore security rules updated
- [ ] `google-services.json` file is in `app/` directory
- [ ] App is connected to the correct Firebase project

## Common Errors and Solutions

### CONFIGURATION_NOT_FOUND
- **Solution**: Enable Email/Password in Firebase Console (see above)

### PERMISSION_DENIED
- **Solution**: Check Firestore security rules

### Network Error
- **Solution**: Check internet connection and Firebase project status

### Invalid API Key
- **Solution**: Regenerate `google-services.json` from Firebase Console

