const PROXY_CONFIG = {
  "**": {
    "target": "http://localhost:9000",
    "secure": false,
    "bypass": function (req) {
      if (req && !(req.url.startsWith("/mooncal") || req.url.startsWith("/suggestBetterTranslation"))) {
        console.log("Serving " + req.url + " from UI");
        return req.url;
      }
      console.log("Serving " + req.url + " from backend");
    }
  }
};

module.exports = PROXY_CONFIG;
