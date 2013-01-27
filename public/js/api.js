var S3Index = {
    
  markerStack: new Array(),
  
  prefexesStack: new Array(),
  
  JSONP : {
    currentScript : null,
    getJSON : function(url, callback, data) {
      var src = url + (url.indexOf("?") + 1 ? "&" : "?");
      var head = document.getElementsByTagName("head")[0];
      var newScript = document.createElement("script");
      var params = [];
      var param_name = ""

      this.success = callback;

      data["callback"] = "S3Index.JSONP.success";
      for (param_name in data) {
        params.push(param_name + "=" + encodeURIComponent(data[param_name]));
      }
      src += params.join("&")

      newScript.type = "text/javascript";
      newScript.src = src;

      if (this.currentScript)
        head.removeChild(currentScript);
      head.appendChild(newScript);
    },
    success : null
  },

  load: function(prefix, marker) {
    prefix = prefix || ''
    marker = marker || ''
    var indexid = document.getElementById('s3index-root').getAttribute('indexid');
    if(!indexid) return;
    S3Index.JSONP.getJSON("/jsonp", function(data) {
      document.getElementById('s3index-root').innerHTML = data.html
    }, {'indexid' : indexid, 'prefix' : prefix, 'marker': marker }  );
    S3Index.currentMarker = marker
    S3Index.currentPrefix = prefix
  },
  
  next: function(marker) {
    S3Index.markerStack.push(S3Index.currentMarker)
    S3Index.load(S3Index.currentPrefix, marker)
  },
  
  prev: function() {
    marker = S3Index.markerStack.pop()
    S3Index.load(S3Index.currentPrefix, marker)
  },
  
  dir: function(prefix) {
    S3Index.markerStack.push(S3Index.currentMarker)
    S3Index.prefexesStack.push(S3Index.currentPrefix)
    S3Index.load(prefix)
  },
  
  back: function() {
    marker = S3Index.markerStack.pop()
    prefix = S3Index.prefexesStack.pop()
    S3Index.load(prefix, marker)
  }
  
  
};