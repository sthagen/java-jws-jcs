## Combining "Detached" JWS with JCS (JSON Canonicalization Scheme)
This repository contains a PoC showing how to create "clear text" JSON signatures
by combining detached JWS compact objects with canonicalization.

### Problem Statement
Assume you have a JSON object like the following:
```json
{
  "statement": "Hello signed world!",
  "otherProperties": [2000,true]
}
```
If you would like to sign this object using JWS compact mode you would end-up with something like this:
```code
eyJhbGciOiJFUzI1NiIsImp3ayI6eyJrdHkiOiJFQyIsImNydiI6IlAtMjU2IiwieCI6IlB4bEpRdTlRNmRPdk
00TEtvWlVoMlhJZTktcGRjTGt2S2ZCZlFrMTFTYjAiLCJ5IjoiNklEcXV4cmJkcTVBQmU0LUhRNzhfZGhNNmVF
QlVidkR0ZHFLMzFZZlJQOCJ9fQ.eyJvdGhlclByb3BlcnRpZXMiOlsyMDAwLHRydWVdLCJzdGF0ZW1lbnQiOiJ
IZWxsbyBzaWduZWQgd29ybGQhIn0.NtipYry9O9A3kdUH4LoRiEuUyIhaCakqfwKh6GAZpnDRUZRGOjiqmYh1G
a9ueSvp2VtiaIL27LrDM47It4WTAA
```
That's not very cool since one of the major benefits of text based schemes (*human readability*), got lost in the process.
### Clear Text Signatures
By rather using JWS in "detached" mode you can reap the benefits of text based schemes while keeping existing security standards!  
```json
{
  "statement": "Hello signed world!",
  "otherProperties": [2000,true],
  "signature": "eyJhbGciOiJFUzI1NiIsImp3ayI6eyJrdHkiOiJFQyIsImNydiI6IlAtMjU2IiwieCI6IlB4bEpRdTlRNmRPdk00TEtvWlVoMlhJZTktcGRjTGt2S2ZCZlFrMTFTYjAiLCJ5IjoiNklEcXV4cmJkcTVBQmU0LUhRNzhfZGhNNmVFQlVidkR0ZHFLMzFZZlJQOCJ9fQ..NtipYry9O9A3kdUH4LoRiEuUyIhaCakqfwKh6GAZpnDRUZRGOjiqmYh1Ga9ueSvp2VtiaIL27LrDM47It4WTAA"
}
```
You may wonder why this is not already described in the JWS standard, right?  Because JSON doesn't require
object properties to be in any specific order as well as having multiple ways of representing the same data, 
you must apply a simple filter process to the original object in order to create a *robust and platform 
independent representation* of the JWS "payload".  If applied to the sample you would get:
```json
{"otherProperties":[2000,true],"statement":"Hello signed world!"}
```
In this *deliberately simple sample*, only the order of the properties needed "correction".  Note that this method
is *internal to the signatures process*; the "wire format" remains unaffected.

The knowledgeable reader probably realizes that this is quite similar to using an HTTP header for holding a detached JWS object.
The primary advantages of this scheme versus using HTTP headers include:
- Due to *transport independence*, signed objects can (for example) be used in browsers
- Signed objects can be *stored* in databases without losing the signature
- Signed objects can be *embedded* in other JSON objects since they conform to JSON

### On Line Demo
If you want to test the signature scheme without any installation or downloading, a simple
demo is currently available at: https://mobilepki.org/jws-jcs/home

### Detailed Description

