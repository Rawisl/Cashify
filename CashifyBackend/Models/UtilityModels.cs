namespace CashifyBackend.Models;

public record ScanRequest(string OcrText);

public record CloudinarySignatureResponse(
    string Signature,
    long Timestamp,
    string ApiKey,
    string CloudName,
    string Folder
);