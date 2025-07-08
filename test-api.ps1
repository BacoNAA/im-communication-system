$response = Invoke-WebRequest -Uri 'http://localhost:8080/api/auth/register' -Method POST -ContentType 'application/json' -InFile 'test-request.json'
Write-Host "Status Code: $($response.StatusCode)"
Write-Host "Response Content: $($response.Content)"