# Free Image Storage Solution

## Overview
Since Firebase Storage requires a paid plan, this implementation uses a **completely free alternative**: storing compressed images as base64 strings in Firestore.

## How It Works

### Image Processing
1. **Compression**: Images are automatically compressed using:
   - Maximum dimensions: 800x800 pixels
   - JPEG quality: 80%
   - Aggressive compression fallback if still too large (400x400 pixels)

2. **Base64 Encoding**: Compressed images are converted to base64 strings

3. **Storage**: 
   - Delivery images (proof of delivery, signatures, COD receipts) → Stored in `images` collection
   - Profile photos and licenses → Stored directly in `drivers` collection as base64 fields

### Firestore Structure

#### Images Collection
```
images/
  {imageId}/
    deliveryId: string
    type: "proof_of_delivery" | "signature" | "cod_receipt"
    base64: string (compressed image)
    createdAt: timestamp
```

#### Drivers Collection
```
drivers/
  {driverId}/
    ...
    profilePhotoBase64: string (optional)
    drivingLicenseBase64: string (optional)
```

#### Deliveries Collection
```
deliveries/
  {deliveryId}/
    ...
    proofOfDeliveryImageId: string (reference to images collection)
    signatureImageId: string (reference to images collection)
```

## Benefits

✅ **Completely Free** - Uses only Firestore free tier
✅ **No Storage Costs** - No Firebase Storage charges
✅ **Offline Support** - Images sync with Firestore offline persistence
✅ **Automatic Compression** - Images optimized automatically
✅ **Simple Implementation** - No additional services needed

## Limitations

⚠️ **Firestore Document Size**: 1MB limit per document
- Images are compressed to stay well under this limit
- Typical compressed image size: 50-200KB

⚠️ **Read Costs**: Each image retrieval counts as a Firestore read
- Free tier: 50,000 reads/day
- Paid tier: $0.06 per 100,000 reads

⚠️ **Storage Costs**: Base64 strings stored in Firestore
- Free tier: 1GB storage
- Paid tier: $0.18 per GB/month

## Usage Example

```kotlin
// Upload proof of delivery
val storageRepository = StorageRepository(context)
val result = storageRepository.uploadProofOfDelivery(imageUri, deliveryId)

result.getOrNull()?.let { imageId ->
    // Update delivery with image reference
    deliveryRepository.updateDeliveryStatus(
        deliveryId,
        DeliveryStatus.DELIVERED,
        proofOfDeliveryImageId = imageId
    )
}

// Retrieve image
val base64 = storageRepository.getProofOfDeliveryBase64(deliveryId)
val bitmap = ImageUtils.base64ToBitmap(base64)
```

## Image Compression Details

- **Initial Compression**: 800x800px max, 80% JPEG quality
- **Fallback Compression**: 400x400px if still too large
- **Target Size**: Under 900KB base64 string (~675KB binary)
- **Format**: JPEG only (best compression for photos)

## Migration from Firebase Storage

If you later want to migrate to Firebase Storage:
1. Images are already stored with metadata (deliveryId, type, createdAt)
2. Can write a migration script to upload base64 → Storage
3. Update imageId references to Storage URLs
4. No data loss during migration

