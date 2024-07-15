== Google Translate Script Deployment ID

To run tests or for a special kind of deployment a deployment ID for translating with a Google Script can be provided in a file.
Create a file next to this one with the ending `.txt` and put the deployment ID in it (an approximately 72 character long string probably base 64 encoded).

=== Get Deployment ID
Based on this [answer on StackOverflow](https://stackoverflow.com/a/48159904/1937795)
- https://www.google.com/script/start/
- Create new script
```
var mock = {
  parameter:{
    q:'hello',
    source:'en',
    target:'fr'
  }
};


function doGet(e) {
  e = e || mock;

  var sourceText = ''
  if (e.parameter.q){
    sourceText = e.parameter.q;
  }

  var sourceLang = '';
  if (e.parameter.source){
    sourceLang = e.parameter.source;
  }

  var targetLang = 'en';
  if (e.parameter.target){
    targetLang = e.parameter.target;
  }

  var translatedText = LanguageApp.translate(sourceText, sourceLang, targetLang, {contentType: 'html'});

  return ContentService.createTextOutput(translatedText).setMimeType(ContentService.MimeType.JSON);
}
```
- Publish
- Deploy as webapp
- Who has access to the app: Anyone even anonymous
- Deploy
- Copy the deployment ID (approximately 72 character long string probably base 64 encoded)
