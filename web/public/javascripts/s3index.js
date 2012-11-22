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

function registerFormSubmitButton(buttonId, targetUrl, nextPageUrl) {
	registerSpinner($("form").attr('id'))
	$('#' + buttonId).click(
			function(event) {
				//cleanup
				$('.s3index-error-msg').each(function(index) {
					$(this).remove();
				});
				$('.error').each(function(index) {
					$(this).removeClass('error');
				});
				//submit form
				var $form = $("form"),
				$inputs = $form.find("input:not(:disabled), select:not(:disabled), button:not(:disabled), textarea:not(:disabled)"),
				serializedData = $form.serialize();
				
				$('input[type=checkbox]').each(function() {     
				    if (this.checked) {

				    }
				    else {
				      serializedData += '&'+this.name+'=off';
				    }
				});

				$inputs.attr("disabled", "disabled");

				$.ajax({
					url : targetUrl,
					type : "post",
					data : serializedData,
					timeout: 10000,
					success : function(response, textStatus, jqXHR) {
						window.location.href = nextPageUrl
					},
					error : function(jqXHR, textStatus, errorThrown) {
					  console.log("Error: " + errorThrown + ", json=" + jqXHR.responseText + ", status=" + textStatus);
					  try
					  {
					    var responseJSON = jQuery.parseJSON(jqXHR.responseText);
                        $.each(responseJSON, function (i, err) {
                          $('.control-group').has('input[name="' + err.elementId + '"]').addClass('error')
                          $('.control-group').has('input[name="' + err.elementId + '"]').append('<span class="s3index-error-msg help-inline">' + err.errorMessage + '</span>');
                        });
					  }
					  catch(e)
					  {
					    if(!jqXHR.responseText) displayContent(e);
					    else displayContent(jqXHR.responseText);
					  }
					},
					complete : function() {
						$inputs.removeAttr("disabled");
					}
				});
				event.preventDefault();
			});
}

function registerSpinner(targetElementId){
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
		var target = document.getElementById(targetElementId);
		var spinner = new Spinner(opts)
		$('form').ajaxStart(function() {
			spinner.spin(target);
		}).ajaxStop(function() {
			spinner.stop();
		});
}

function loadProperties(url) {
  $.ajax({
    url : url,
    cache : false,
    success : function(response, textStatus, jqXHR) {
      console.log("Resp: " + response + ", json=" + jqXHR.responseText + ", status=" + textStatus);
      applyProperties(jQuery.parseJSON(jqXHR.responseText))
    },
    error : function(jqXHR, textStatus, errorThrown) {
      displayContent(responseText);
    }
  });
}

function displayContent(content){
  document.open();
  document.write(content);
  document.close();
}
