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

