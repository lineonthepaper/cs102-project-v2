#!/bin/bash
# Setup script to download required face recognition models
# This script is automatically called during backend startup, or can be run manually

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Get script directory and project root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
MODELS_DIR="$PROJECT_ROOT/backend/src/main/resources/models"

echo -e "${GREEN}=== Face Recognition Models Setup ===${NC}"
echo ""

# Create models directory if it doesn't exist
mkdir -p "$MODELS_DIR"

# Check and download facenet.onnx (ArcFace ResNet100)
MODEL_FILE="$MODELS_DIR/facenet.onnx"
MODEL_URL="https://github.com/onnx/models/raw/main/validated/vision/body_analysis/arcface/model/arcfaceresnet100-8.onnx"
EXPECTED_SIZE_MB=249

if [ -f "$MODEL_FILE" ]; then
    FILE_SIZE=$(du -m "$MODEL_FILE" | cut -f1)
    if [ "$FILE_SIZE" -ge 200 ]; then
        echo -e "${GREEN}✓${NC} ArcFace model found (${FILE_SIZE} MB)"
    else
        echo -e "${YELLOW}⚠${NC}  Model file exists but seems incomplete (${FILE_SIZE} MB, expected ~${EXPECTED_SIZE_MB} MB)"
        echo "Downloading fresh copy..."
        rm -f "$MODEL_FILE"
        DOWNLOAD_NEEDED=true
    fi
else
    echo -e "${YELLOW}⚠${NC}  ArcFace model not found"
    DOWNLOAD_NEEDED=true
fi

if [ "$DOWNLOAD_NEEDED" = true ]; then
    echo -e "${YELLOW}Downloading ArcFace ResNet100 model (~${EXPECTED_SIZE_MB} MB)...${NC}"
    echo "This may take a few minutes depending on your internet connection."
    echo ""
    
    # Try curl first (available on macOS/Linux)
    if command -v curl &> /dev/null; then
        if curl -L --progress-bar -o "$MODEL_FILE" "$MODEL_URL"; then
            FILE_SIZE=$(du -m "$MODEL_FILE" | cut -f1)
            if [ "$FILE_SIZE" -ge 200 ]; then
                echo -e "${GREEN}✓${NC} Successfully downloaded ArcFace model (${FILE_SIZE} MB)"
            else
                echo -e "${RED}✗${NC} Download seems incomplete. Please run this script again."
                rm -f "$MODEL_FILE"
                exit 1
            fi
        else
            echo -e "${RED}✗${NC} Download failed. Please check your internet connection and try again."
            exit 1
        fi
    # Fallback to wget
    elif command -v wget &> /dev/null; then
        if wget --progress=bar:force -O "$MODEL_FILE" "$MODEL_URL"; then
            FILE_SIZE=$(du -m "$MODEL_FILE" | cut -f1)
            if [ "$FILE_SIZE" -ge 200 ]; then
                echo -e "${GREEN}✓${NC} Successfully downloaded ArcFace model (${FILE_SIZE} MB)"
            else
                echo -e "${RED}✗${NC} Download seems incomplete. Please run this script again."
                rm -f "$MODEL_FILE"
                exit 1
            fi
        else
            echo -e "${RED}✗${NC} Download failed. Please check your internet connection and try again."
            exit 1
        fi
    else
        echo -e "${RED}✗${NC} Neither curl nor wget is available. Please install one of them."
        echo ""
        echo "Manual download:"
        echo "  1. Visit: $MODEL_URL"
        echo "  2. Download the file"
        echo "  3. Save it as: $MODEL_FILE"
        exit 1
    fi
fi

# Verify Haar Cascade exists (should already be in repo)
HAAR_FILE="$MODELS_DIR/haarcascade_frontalface_alt.xml"
if [ -f "$HAAR_FILE" ]; then
    echo -e "${GREEN}✓${NC} Haar Cascade face detector found"
else
    echo -e "${YELLOW}⚠${NC}  Haar Cascade not found (this should be in the repository)"
fi

echo ""
echo -e "${GREEN}=== Setup Complete ===${NC}"
echo ""
echo "All required models are ready. You can now start the backend server."

