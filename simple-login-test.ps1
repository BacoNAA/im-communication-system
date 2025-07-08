# Simple Login Lock Test
$testEmail = "test.lock@example.com"
$loginUrl = "http://localhost:8080/api/auth/login/password"
$wrongPassword = "wrongpassword"

Write-Host "Testing login failure tracking..." -ForegroundColor Cyan
Write-Host "Email: $testEmail" -ForegroundColor Yellow

for ($i = 1; $i -le 3; $i++) {
    Write-Host "\n--- Attempt $i ---" -ForegroundColor Yellow
    
    $body = @{
        email = $testEmail
        password = $wrongPassword
        deviceType = "web"
    } | ConvertTo-Json
    
    try {
        $response = Invoke-WebRequest -Uri $loginUrl -Method POST -Body $body -ContentType "application/json" -UseBasicParsing
        Write-Host "Success: $($response.StatusCode)" -ForegroundColor Green
        Write-Host "Response: $($response.Content)" -ForegroundColor Green
    } catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        Write-Host "Status Code: $statusCode" -ForegroundColor Red
        
        if ($_.Exception.Response) {
            $stream = $_.Exception.Response.GetResponseStream()
            $reader = New-Object System.IO.StreamReader($stream)
            $content = $reader.ReadToEnd()
            Write-Host "Error Response: $content" -ForegroundColor Red
            $reader.Close()
            $stream.Close()
        }
    }
}

Write-Host "\nTest completed. Check the responses above to verify:" -ForegroundColor Cyan
Write-Host "1. Failed attempts are being tracked" -ForegroundColor Yellow
Write-Host "2. Remaining attempts are decreasing" -ForegroundColor Yellow
Write-Host "3. Error messages show proper feedback" -ForegroundColor Yellow