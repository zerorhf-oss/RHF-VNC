#!/data/data/com.termux/files/usr/bin/bash
# swap.sh — ganti Supabase + wallet, push, auto build
# Usage:
#   bash swap.sh <SUPABASE_URL> <SUPABASE_KEY> [SOL_WALLET]
DIR="$(cd "$(dirname "$0")" && pwd)"
CFG="$DIR/app/src/main/java/com/rhfvnc/strain/Config.java"
URL="$1"; KEY="$2"; WALLET="${3:-}"
if [ -z "$URL" ] || [ -z "$KEY" ]; then
    echo "Usage: bash swap.sh <SUPABASE_URL> <SUPABASE_KEY> [SOL_WALLET]"
    exit 1
fi
sed -i "s|SUPABASE_URL = \"[^\"]*\"|SUPABASE_URL = \"$URL\"|" "$CFG"
sed -i "s|SUPABASE_KEY = \"[^\"]*\"|SUPABASE_KEY = \"$KEY\"|" "$CFG"
if [ -n "$WALLET" ]; then
    sed -i "s|SOL_WALLET = \"[^\"]*\"|SOL_WALLET = \"$WALLET\"|" "$CFG"
fi
echo "[+] Config updated:"
grep -E "SUPABASE_URL|SUPABASE_KEY|SOL_WALLET" "$CFG"
cd "$DIR"
git add .
git commit -m "swap: supabase/wallet $(date +%Y%m%d_%H%M)"
git push
echo ""
echo "[+] Pushed. GitHub Actions akan build APK baru."
