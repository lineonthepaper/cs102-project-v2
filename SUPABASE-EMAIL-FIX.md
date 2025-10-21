# Supabase Email Validation Issue - Quick Fix

## Problem
Supabase Auth rejects certain email domains including `.sg` (Singapore) domains:
- `student1@smu.edu.sg` ❌ Invalid
- `instructor@company.sg` ❌ Invalid

**Error:** `Email address "student1@smu.edu.sg" is invalid`

## Solutions

### Option 1: Use Accepted Email Domains (Recommended)

Use these email domains instead:
- `.com` - `student1@smu.edu.com` ✅
- `.edu` - `student1@smu.edu` ✅  
- `.org` - `student1@university.org` ✅
- `.net` - `instructor@company.net` ✅

### Option 2: Disable Email Confirmations in Supabase (For Development Only)

1. Go to https://supabase.com/dashboard
2. Select your project (`ihesechgwrelxkwizcke`)
3. Go to **Authentication** → **Providers** → **Email**
4. **Disable "Confirm email"**
5. Save changes

⚠️ **Warning:** This allows any email format but reduces security. Only use for development/testing!

### Option 3: Configure Custom SMTP (Advanced)

Configure your own email provider that accepts `.sg` domains:
1. Go to Supabase Dashboard → Authentication → Email Templates
2. Set up custom SMTP settings
3. Use a provider that accepts all TLDs

## Rate Limiting

**Error:** `For security purposes, you can only request this after 46 seconds`

**Solution:** Wait between creating accounts. Supabase limits:
- **5 signups per hour** from same IP
- **30 seconds minimum** between signups

To avoid this:
1. Create accounts slowly (wait 1 minute between each)
2. Use different email providers
3. Or increase limits in Supabase Dashboard → Authentication → Rate Limits (paid plans only)

## Quick Test

Try these test emails instead:
```
instructor1@test.com
ta1@test.com  
student1@test.com
admin@test.edu
```

## For Production

In production, you should:
1. Use your institution's actual email domain
2. Configure email confirmation properly
3. Set appropriate rate limits
4. Use custom SMTP if needed

---

**Updated:** The frontend now shows better error messages when invalid email domains are used.

