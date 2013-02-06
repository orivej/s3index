$.fn.serializeObject = function()
{
    var o = {};
    var a = this.serializeArray();
    $.each(a, function() {
        if (o[this.name] !== undefined) {
            if (!o[this.name].push) {
                o[this.name] = [o[this.name]];
            }
            o[this.name].push(this.value || '');
        } else {
            o[this.name] = this.value || '';
        }
    });
    return o;
};

function clone(element, container) {
	var num = container.children().length;
	var newNum = new Number(num);

	var newElem = element.clone();
	newElem.find('[id]').each(function(index) {
		$(this).attr('id', $(this).attr('id') + newNum);
	});
	container.append(newElem);
	$(newElem.find('.clonable-remove-button:first')).click(function() {
	    newElem.remove();
	});
}

function registerClonable() {
	$('.clonable').each(function(index) {
	    var element = $(this).clone();
    	var addButton = element.find('.clonable-add-button:first').clone();
    	element.find('.clonable-add-button:first').remove();
    	var container = $('<div class="clonable-container"></div>')	
    	$('.clonable:first').after(addButton);
    	$('.clonable:first').after(container);
    	$('.clonable:first').remove();
    	addButton.click(function() {
    		clone(element, container)
    	});
	});
}

function postProperties(targetUrl, successHandler, errorHandler) {
  
  errorHandler = errorHandler || parseBadRequestErrors
  successHandler = successHandler || parseBadRequestWarnings
  
  // cleanup
  $('.s3index-error-msg').each(function(index) {
    $(this).remove();
  });
  $('.error').each(function(index) {
    $(this).removeClass('error');
  });
  $('.warning').each(function(index) {
    $(this).removeClass('warning');
  });
  // submit form
  var $form = $("form"), $inputs = $form.find("input:not(:disabled), select:not(:disabled), button:not(:disabled), textarea:not(:disabled)"), serializedData = $form.serializeObject()

  $inputs.attr("disabled", "disabled");

  post(targetUrl, serializedData, successHandler, errorHandler, function() {$inputs.removeAttr("disabled")})
}

function post(targetUrl, data, successHandler, errorHandler, completeHandler){
  $.ajax({
    url : targetUrl,
    type : "post",
    data : JSON.stringify(data),
    contentType: "application/json; charset=utf-8",
    dataType: "json",
    timeout : 10000,
    success : function(response, textStatus, jqXHR) {
      successHandler(jqXHR.responseText)
    },
    error : function(jqXHR, textStatus, errorThrown) {
      errorHandler(jqXHR.responseText)
    },
    complete : function() {
      completeHandler();
    }
  });
  event.preventDefault();
}

function parseBadRequestErrors(json){
  parseBadRequest(json, 'error');
}

function parseBadRequestWarnings(json){
  parseBadRequest(json, 'warning');
}

function parseBadRequest(json, errorClassName){
  var errorClassName = errorClassName || 'error'
  if(json){
    try {
      $.each(jQuery.parseJSON(json), function(elementId, errorMessage) {
        if(elementId && errorMessage){
          $('.control-group').has('input[name="' + elementId + '"]').addClass(errorClassName)
          $('.control-group').has('input[name="' + elementId + '"]').append('<span class="s3index-error-msg help-inline">' + errorMessage + '</span>');
        }
      });
    } catch (e) {
      if (!json)
        displayContent(e);
      else
        displayContent(json);
    }
  }
}

function getProperties(url) {
  loadData(url,
    function(responseText) {
      applyProperties(jQuery.parseJSON(responseText))
    },
    function(responseText) {
      displayContent(responseText);
    }
  );
}

function registerSpinner(){
	var opts = {
			lines : 13, // The number of lines to draw
			length : 7, // The length of each line
			width : 4, // The line thickness
			radius : 10, // The radius of the inner circle
			corners : 1, // Corner roundness (0..1)
			rotate : 0, // The rotation offset
			color : '#000', // #rgb or #rrggbb
			speed : 1, // Rounds per second
			trail : 60, // Afterglow percentage
			shadow : false, // Whether to render a shadow
			hwaccel : false, // Whether to use hardware acceleration
			className : 'spinner', // The CSS class to assign to the spinner
			zIndex : 2e9, // The z-index (defaults to 2000000000)
			top : 'auto', // Top position relative to parent in px
			left : 'auto' // Left position relative to parent in px
		};
		$(document).ajaxStart(function() {
		  $('form').spin(opts)
		}).ajaxStop(function() {
		  $("form").spin(false);
		});
}

function loadData(url, success, failure) {
  $.ajax({
    url : url,
    cache : false,
    success : function(response, textStatus, jqXHR) {
      success(jqXHR.responseText)
    },
    error : function(jqXHR, textStatus, errorThrown) {
      failure(jqXHR.responseText)
    }
  });
}

function displayContent(content){
  document.open();
  document.write(content);
  document.close();
}

(function($) {
  $.fn.spin = function(opts, color) {
      var presets = {
          "tiny": { lines: 8, length: 2, width: 2, radius: 3 },
          "small": { lines: 8, length: 4, width: 3, radius: 5 },
          "large": { lines: 10, length: 8, width: 4, radius: 8 }
      };
      if (Spinner) {
          return this.each(function() {
              var $this = $(this),
                  data = $this.data();
              
              if (data.spinner) {
                  data.spinner.stop();
                  delete data.spinner;
              }
              if (opts !== false) {
                  if (typeof opts === "string") {
                      if (opts in presets) {
                          opts = presets[opts];
                      } else {
                          opts = {};
                      }
                      if (color) {
                          opts.color = color;
                      }
                  }
                  data.spinner = new Spinner($.extend({color: $this.css('color')}, opts)).spin(this);
              }
          });
      } else {
          throw "Spinner class not available.";
      }
  };
})(jQuery);
