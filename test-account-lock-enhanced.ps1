# Test Enhanced Account Lock Functionality
# Verify detailed error messages and improved user experience

$baseUrl = "http://localhost:8080"
$loginUrl = "$baseUrl/api/auth/login/password"
$testEmail = "test.lock@example.com"
$wrongPassword = "wrongpassword"

Write-Host "=== Testing Enhanced Account Lock Functionality ===" -ForegroundColor Green
Write-Host "Test Email: $testEmail" -ForegroundColor Yellow
Write-Host "Wrong Password: $wrongPassword" -ForegroundColor Yellow
Write-Host ""

# Prepare request body
$requestBody = @{
    email = $testEmail
    password = $wrongPassword
}
$requestBodyJson = $requestBody | ConvertTo-Json

# Test multiple login failures
for ($i = 1; $i -le 7; $i++) {
    Write-Host "=== Attempt $i ===" -ForegroundColor Cyan
    
    try {
        $response = Invoke-WebRequest -Uri $loginUrl -Method POST -ContentType "application/json" -Body $requestBodyJson -ErrorAction Stop
        Write-Host "Unexpected Success: $($response.StatusCode)" -ForegroundColor Red
        Write-Host "Response: $($response.Content)" -ForegroundColor Red
    }
    catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        Write-Host "Status Code: $statusCode" -ForegroundColor Yellow
        
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
    
    if ($i -eq 5) {
        Write-Host "Account should be locked now, waiting 5 seconds..." -ForegroundColor Magenta
        Start-Sleep -Seconds 5
    } elseif ($i -lt 7) {
        Start-Sleep -Seconds 1
    }
}

Write-Host "=== Test Complete ===" -ForegroundColor Green
Write-Host "Please check log files for detailed security logs" -ForegroundColor Yellow