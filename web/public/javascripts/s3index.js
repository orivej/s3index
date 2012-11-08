function clone(classId, removeButtonId) {
	var clonedElement = $('.' + classId);
	var first = $('.' + classId + ':first');
	var last = $('.' + classId + ':last');
	var num = clonedElement.size() - 1;
	var newNum = new Number(num + 1);

	var newElem = first.clone().removeAttr('hidden');
	newElem.find('[id]').each(function(index) {
		$(this).attr('id', $(this).attr('id') + newNum);
	});
	newElem.find('[name]').each(function(index) {
		$(this).attr('name', $(this).attr('name') + newNum);
	});
	last.after(newElem.attr('id', classId + newNum));
	$('#' + removeButtonId + newNum).click(function() {
		$('#' + classId + newNum).remove();
	});
}

function registerClonable(classId, addButtonId, removeButtonId) {
	$('.' + classId + ':first').attr('hidden', 'true')
	$('#' + addButtonId).click(function() {
		clone(classId, removeButtonId)
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
				$inputs = $form.find("input, select, button, textarea"),
				serializedData = $form.serialize();

				$inputs.attr("disabled", "disabled");

				$.ajax({
					url : targetUrl,
					type : "post",
					data : serializedData,
					success : function(response, textStatus, jqXHR) {
						console.log("Hooray, it worked!");
					},
					error : function(jqXHR, textStatus, errorThrown) {
						var responseJSON = jQuery.parseJSON(jqXHR.responseText);
						$.each(responseJSON, function (i, err) {
							$('#CG' + err.errorId).addClass('error')
						    $('#CG' + err.errorId).append('<span class="s3index-error-msg help-inline">' + err.errorMessage + '</span>');
						});
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
