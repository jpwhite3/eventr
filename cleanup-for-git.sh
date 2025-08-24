#!/bin/bash

# Cleanup script to prepare repository for GitHub
# This script removes files that should not be committed to version control

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_status "🧹 Cleaning up repository for GitHub..."

# Function to safely remove if exists
safe_remove() {
    if [ -e "$1" ]; then
        rm -rf "$1"
        print_success "Removed $1"
    fi
}

# Remove node_modules directories
print_status "Removing node_modules directories..."
find . -name "node_modules" -type d -exec rm -rf {} + 2>/dev/null || true

# Remove log files
print_status "Removing log files..."
find . -name "*.log" -type f -delete 2>/dev/null || true
find . -name "npm-debug.log*" -type f -delete 2>/dev/null || true
find . -name "yarn-debug.log*" -type f -delete 2>/dev/null || true
find . -name "yarn-error.log*" -type f -delete 2>/dev/null || true

# Remove OS-specific files
print_status "Removing OS-specific files..."
find . -name ".DS_Store" -type f -delete 2>/dev/null || true
find . -name "Thumbs.db" -type f -delete 2>/dev/null || true
find . -name ".AppleDouble" -type d -exec rm -rf {} + 2>/dev/null || true

# Remove IDE-specific files that might have been missed
print_status "Removing IDE files..."
safe_remove ".vscode"
safe_remove ".idea"
find . -name "*.iml" -type f -delete 2>/dev/null || true
find . -name "*.iws" -type f -delete 2>/dev/null || true
find . -name "*.ipr" -type f -delete 2>/dev/null || true

# Remove build artifacts
print_status "Removing build artifacts..."
safe_remove "target"
safe_remove "build"
find . -name "*.class" -type f -delete 2>/dev/null || true

# Remove temporary files
print_status "Removing temporary files..."
find . -name "*.tmp" -type f -delete 2>/dev/null || true
find . -name "*.temp" -type f -delete 2>/dev/null || true
safe_remove "tmp"
safe_remove "temp"

# Remove coverage directories
print_status "Removing coverage files..."
find . -name "coverage" -type d -exec rm -rf {} + 2>/dev/null || true
find . -name ".nyc_output" -type d -exec rm -rf {} + 2>/dev/null || true
find . -name "*.lcov" -type f -delete 2>/dev/null || true

# Remove cache directories
print_status "Removing cache directories..."
find . -name ".cache" -type d -exec rm -rf {} + 2>/dev/null || true
find . -name ".parcel-cache" -type d -exec rm -rf {} + 2>/dev/null || true
find . -name ".npm" -type d -exec rm -rf {} + 2>/dev/null || true
find . -name ".eslintcache" -type f -delete 2>/dev/null || true

# Remove database files
print_status "Removing database files..."
find . -name "*.db" -type f -delete 2>/dev/null || true
find . -name "*.sqlite" -type f -delete 2>/dev/null || true
find . -name "*.sqlite3" -type f -delete 2>/dev/null || true
find . -name "*.h2.db" -type f -delete 2>/dev/null || true
find . -name "*.mv.db" -type f -delete 2>/dev/null || true
find . -name "*.trace.db" -type f -delete 2>/dev/null || true

# Remove environment files with secrets
print_status "Removing environment files..."
find . -name ".env.local" -type f -delete 2>/dev/null || true
find . -name ".env.development.local" -type f -delete 2>/dev/null || true
find . -name ".env.test.local" -type f -delete 2>/dev/null || true
find . -name ".env.production.local" -type f -delete 2>/dev/null || true
safe_remove "application-secrets.yml"
safe_remove "application-prod.yml"

print_success "🎉 Repository cleanup completed!"

print_status "📝 Summary of actions taken:"
echo "  • Removed all node_modules directories"
echo "  • Removed log files and debug logs"
echo "  • Removed OS-specific files (.DS_Store, Thumbs.db)"
echo "  • Removed IDE configuration files"
echo "  • Removed build artifacts and temporary files"
echo "  • Removed test coverage files"
echo "  • Removed cache directories"
echo "  • Removed local database files"
echo "  • Removed environment files with secrets"

print_status "🔍 Checking git status..."
if command -v git >/dev/null 2>&1 && git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
    echo ""
    echo "Git status after cleanup:"
    git status --porcelain | head -20
    
    if [ "$(git status --porcelain | wc -l)" -gt 20 ]; then
        echo "... and $(echo $(($(git status --porcelain | wc -l) - 20))) more files"
    fi
    
    print_status "💡 Next steps:"
    echo "1. Review the changes: git status"
    echo "2. Add files to commit: git add ."
    echo "3. Commit changes: git commit -m 'Add comprehensive documentation and webhook test client'"
    echo "4. Push to GitHub: git push origin main"
else
    print_warning "Not in a git repository. Initialize with: git init"
fi

print_status "✨ Repository is ready for GitHub!"