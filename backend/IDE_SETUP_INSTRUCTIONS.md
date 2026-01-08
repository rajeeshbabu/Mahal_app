# Setting Environment Variables in IDE

The error `Could not resolve placeholder 'RAZORPAY_KEY_ID'` means Spring Boot can't find the environment variables. This happens because IDEs don't automatically pick up environment variables set by scripts.

## Solution: Set Environment Variables in Your IDE

### Option 1: IntelliJ IDEA

1. **Go to Run Configuration:**
   - Click on the dropdown next to the Run button (top right)
   - Select **Edit Configurations...**
   - OR: Right-click `MahalBackendApplication.java` → **Modify Run Configuration...**

2. **Add Environment Variables:**
   - Find your `MahalBackendApplication` configuration
   - Look for **Environment variables** field
   - Click the folder icon or **Browse...** button

3. **Add the variables:**
   - Click **+** (plus icon) to add a new variable
   - Add these two variables:
     ```
     Name: RAZORPAY_KEY_ID
     Value: rzp_test_RxJksqriVHTcTu
     ```
     ```
     Name: RAZORPAY_KEY_SECRET
     Value: 1bF6nphPQ174RoEAiE8qAOye
     ```
   - Click **OK** to save

4. **Run the application** again

---

### Option 2: Eclipse/STS

1. **Go to Run Configuration:**
   - Right-click `MahalBackendApplication.java`
   - Select **Run As** → **Run Configurations...**

2. **Add Environment Variables:**
   - Select your Spring Boot configuration (or create new)
   - Go to **Environment** tab
   - Click **New...**

3. **Add the variables:**
   - **Name:** `RAZORPAY_KEY_ID`
   - **Value:** `rzp_test_RxJksqriVHTcTu`
   - Click **OK**
   - Repeat for `RAZORPAY_KEY_SECRET` = `1bF6nphPQ174RoEAiE8qAOye`

4. **Click Apply**, then **Run**

---

### Option 3: VS Code

1. **Edit `.vscode/launch.json`:**
   - Create `.vscode` folder in project root if it doesn't exist
   - Create/edit `launch.json` file

2. **Add environment variables:**
   ```json
   {
     "version": "0.2.0",
     "configurations": [
       {
         "type": "java",
         "name": "MahalBackendApplication",
         "request": "launch",
         "mainClass": "com.mahal.MahalBackendApplication",
         "projectName": "backend",
         "env": {
           "RAZORPAY_KEY_ID": "rzp_test_RxJksqriVHTcTu",
           "RAZORPAY_KEY_SECRET": "1bF6nphPQ174RoEAiE8qAOye"
         }
       }
     ]
   }
   ```

---

### Option 4: Command Line (Alternative)

If you prefer command line:

1. **Open PowerShell in the `backend` folder**

2. **Run the setup script:**
   ```powershell
   .\setup-razorpay-keys.ps1
   ```

3. **Run Spring Boot from command line:**
   ```powershell
   mvn spring-boot:run
   ```
   OR if you have a JAR:
   ```powershell
   java -jar target/your-app.jar
   ```

**Important:** You must run both commands in the **same PowerShell window** for the environment variables to be available.

---

### Option 5: Create a `.env` file (Not Recommended for Production)

If your IDE supports `.env` files (IntelliJ Ultimate, VS Code with extensions), you can create `backend/.env`:

```
RAZORPAY_KEY_ID=rzp_test_RxJksqriVHTcTu
RAZORPAY_KEY_SECRET=1bF6nphPQ174RoEAiE8qAOye
```

**Warning:** Don't commit `.env` files to git! Add `.env` to `.gitignore`.

---

## Quick Fix for IntelliJ IDEA (Most Common)

1. Click **Run** → **Edit Configurations...** (or click dropdown next to Run button)
2. Select **MahalBackendApplication**
3. Find **Environment variables** field
4. Click the folder icon
5. Click **+** and add:
   - `RAZORPAY_KEY_ID` = `rzp_test_RxJksqriVHTcTu`
   - `RAZORPAY_KEY_SECRET` = `1bF6nphPQ174RoEAiE8qAOye`
6. Click **OK**, then **Apply**
7. Run the application

---

## Verify It Works

After setting environment variables, when you run the application, you should see:
- ✅ No errors about `Could not resolve placeholder 'RAZORPAY_KEY_ID'`
- ✅ Application starts successfully
- ✅ Server runs on port 8080

If you still see errors, double-check:
- Variable names are exactly: `RAZORPAY_KEY_ID` and `RAZORPAY_KEY_SECRET` (case-sensitive)
- No extra spaces in the values
- You've saved/applied the configuration


