# Test Admin Account Lock Management APIs
# Verify account lock status query and management functionality

$baseUrl = "http://localhost:8080"
$testEmail = "test.lock@example.com"
$encodedEmail = [System.Web.HttpUtility]::UrlEncode($testEmail)

Write-Host "=== Testing Admin Account Lock Management APIs ===" -ForegroundColor Green
Write-Host "Test Email: $testEmail" -ForegroundColor Yellow
Write-Host ""

# Function to make API request and display response
function Invoke-AdminAPI {
    param(
        [string]$Url,
        [string]$Method = "GET",
        [string]$Description,
        [string]$Body = $null
    )
    
    Write-Host "=== $Description ===" -ForegroundColor Cyan
    Write-Host "URL: $Url" -ForegroundColor Gray
    Write-Host "Method: $Method" -ForegroundColor Gray
    
    try {
        $headers = @{
            "Content-Type" = "application/json"
        }
        
        if ($Body) {
            Write-Host "Body: $Body" -ForegroundColor Gray
            $response = Invoke-WebRequest -Uri $Url -Method $Method -Body $Body -Headers $headers -ErrorAction Stop
        } else {
            $response = Invoke-WebRequest -Uri $Url -Method $Method -Headers $headers -ErrorAction Stop
        }
        
        Write-Host "Status: $($response.StatusCode)" -ForegroundColor Green
        $responseData = $response.Content | ConvertFrom-Json
        $responseData | ConvertTo-Json -Depth 10 | Write-Host -ForegroundColor Green
    }
    catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        Write-Host "Status Code: $statusCode" -ForegroundColor Red
        
        if ($_.Exception.Response) {
            try {
                $stream = $_.Exception.Response.GetResponseStream()
                $reader = New-Object System.IO.StreamReader($stream)
                $responseBody = $reader.ReadToEnd()
                $reader.Close()
                $stream.Close()
                
                Write-Host "Error Response:" -ForegroundColor Red
                $errorData = $responseBody | ConvertFrom-Json
                $errorData | ConvertTo-Json -Depth 10 | Write-Host -ForegroundColor Red
            }
            catch {
                Write-Host "Cannot parse error response: $($_.Exception.Message)" -ForegroundColor Red
            }
        }
    }
    
    Write-Host ""
}

# Test 1: Query account lock status
Invoke-AdminAPI -Url "$baseUrl/api/account-lock/status/$encodedEmail" -Description "Query Account Lock Status"

# Test 2: Unlock account
$unlockBody = @{ email = $testEmail } | ConvertTo-Json
Invoke-AdminAPI -Url "$baseUrl/api/account-lock/unlock" -Method "POST" -Body $unlockBody -Description "Unlock Account"

# Test 3: Query status after unlock
Invoke-AdminAPI -Url "$baseUrl/api/account-lock/status/$encodedEmail" -Description "Query Status After Unlock"

# Test 4: Clear login failures
$clearBody = @{ email = $testEmail } | ConvertTo-Json
Invoke-AdminAPI -Url "$baseUrl/api/account-lock/clear-failed-attempts" -Method "POST" -Body $clearBody -Description "Clear Login Failures"

# Test 5: Manual lock account
$lockBody = @{ email = $testEmail; reason = "Manual lock for testing" } | ConvertTo-Json
Invoke-AdminAPI -Url "$baseUrl/api/account-lock/lock" -Method "POST" -Body $lockBody -Description "Manual Lock Account"

# Test 6: Query status after manual lock
Invoke-AdminAPI -Url "$baseUrl/api/account-lock/status/$encodedEmail" -Description "Query Status After Manual Lock"

# Test 7: Unlock again
$unlockBody2 = @{ email = $testEmail } | ConvertTo-Json
Invoke-AdminAPI -Url "$baseUrl/api/account-lock/unlock" -Method "POST" -Body $unlockBody2 -Description "Unlock Account Again"

Write-Host "=== Admin API Test Complete ===" -ForegroundColor Green
Write-Host "All account lock management APIs have been tested" -ForegroundColor Yellow