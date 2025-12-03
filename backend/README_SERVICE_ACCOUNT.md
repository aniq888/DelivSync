# Backend Service Account Key Instructions

You need to place your Firebase service account key file in this directory.

## Steps to get the service account key:

1. Go to Firebase Console: https://console.firebase.google.com/
2. Select your project (DelivSync)
3. Click on the gear icon ⚙️ next to "Project Overview"
4. Select "Project settings"
5. Go to the "Service accounts" tab
6. Click "Generate new private key"
7. Download the JSON file
8. Rename it to `serviceAccountKey.json`
9. Place it in this backend folder

⚠️ **IMPORTANT**: Never commit this file to version control!
Add `serviceAccountKey.json` to your `.gitignore` file.

