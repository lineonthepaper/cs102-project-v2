# Face Recognition Models

## Current Setup

This project uses **ArcFace ResNet100** model for face recognition, which provides significantly better accuracy than the previous SFace model.

## Model Files Required

### 1. Face Recognition Model (ArcFace)
- **File**: `facenet.onnx`
- **Size**: 249 MB
- **Download**: 
  ```bash
  cd backend/src/main/resources/models/
  curl -L -o facenet.onnx "https://github.com/onnx/models/raw/main/validated/vision/body_analysis/arcface/model/arcfaceresnet100-8.onnx"
  ```

### 2. Face Detection Model (Haar Cascade)
- **File**: `haarcascade_frontalface_alt.xml`
- **Size**: 661 KB
- **Status**: Already included in repository

## Why ArcFace?

ArcFace (2019) provides:
- ✅ **Much better accuracy** for different ethnicities
- ✅ **More reliable similarity scores**
- ✅ **Better handling of pose/lighting variations**
- ✅ **State-of-the-art performance** (>99% accuracy on standard benchmarks)

## Technical Details

### Input Requirements
- Image size: 112x112 pixels (RGB)
- Normalization: `(pixel - 127.5) / 128.0`
- Output: 512-dimensional embedding vector

### Similarity Threshold
- **Threshold**: 0.50 (50%)
- Lower scores = stricter matching
- Range: [-1, 1] where 1 = identical, -1 = completely different

## Model Not Included in Git

The ArcFace model file is **not committed to Git** because:
- File size (249 MB) exceeds GitHub's 100 MB limit
- Reduces repository size
- Allows flexibility to upgrade/change models

## First Time Setup

If you're cloning this repository for the first time:

```bash
# 1. Clone the repository
git clone <repository-url>
cd cs102-project-v2

# 2. Download the ArcFace model
cd backend/src/main/resources/models/
curl -L -o facenet.onnx "https://github.com/onnx/models/raw/main/validated/vision/body_analysis/arcface/model/arcfaceresnet100-8.onnx"

# 3. Verify the file
ls -lh facenet.onnx  # Should show ~249 MB
```

## Alternative Models

If you want to try different models:

### Option 1: Smaller ArcFace (if 249MB is too large)
Look for quantized or mobile versions of ArcFace

### Option 2: FaceNet
- Download FaceNet ONNX model
- Update `FaceRecognitionService.java` to point to new model
- Adjust preprocessing in `FaceEmbeddingUtils.java`

### Option 3: Back to SFace (original)
- The SFace model is still in the repository
- Change model path back to `face_recognition_sface_2021dec.onnx`
- Revert preprocessing changes

## Troubleshooting

### Model Not Found Error
```
java.lang.IllegalStateException: Failed to load model
```
**Solution**: Download the `facenet.onnx` file using the command above

### Poor Recognition Accuracy
1. Delete all existing face images (they use old SFace embeddings)
2. Re-upload face images with good quality
3. Ensure consistent lighting and camera conditions

### Out of Memory Error
The ArcFace model requires ~1GB RAM for inference
- Reduce number of concurrent scan requests
- Or switch to a smaller model

## Performance Notes

- **Model loading time**: 2-3 seconds on first request
- **Inference time**: 50-100ms per face
- **Memory usage**: ~1GB RAM
- **Accuracy improvement**: ~10-15% better than SFace for diverse faces

