try {
    $body = Get-Content -Path 'test-login.json' -Raw
    $response = Invoke-WebRequest -Uri 'http://localhost:8080/api/auth/login/password' -Method POST -ContentType 'application/json' -Body $body
    Write-Host "Status Code: $($response.StatusCode)"
    Write-Host "Content: $($response.Content)"
} catch {
    Write-Host "Error: $($_.Exception.Message)"
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $responseBody = $reader.ReadToEnd()
        Write-Host "Response Body: $responseBody"
    }
}