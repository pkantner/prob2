bms = (function() {

	var extern = {}
	var session = Session();

	$(document).ready(function() {
		
	    $('#navigation a').stop().animate({'marginLeft':'-85px'},1000);

	    $('#navigation > li').hover(
	        function () {
	            $('a',$(this)).stop().animate({'marginLeft':'-2px'},200);
	        },
	        function () {
	            $('a',$(this)).stop().animate({'marginLeft':'-85px'},200);
	        }
	    );

		$('.template').click(function() {

			$("#sourceModal").on('shown', function() {
				// editorHtml.refresh()
				// editorJavascript.refresh()
			}).on('hidden', function() {
				// renderEdit()
			});
			// Show Modal
			$("#sourceModal").modal('show');

			$(".template").find("a").stop().animate({
				'marginLeft' : '-85px'
			}, 200);

		})
	    
	});



	// --------------------------------------------
	// Helper functions
	// --------------------------------------------
	jQuery.fn.toHtmlString = function() {
		return $('<div></div>').html($(this).clone()).html();
	};

	jQuery.expr[':'].parents = function(a, i, m) {
		return jQuery(a).parents(m[3]).length < 1;
	};

	var readHTMLFile = function(url) {
		var toReturn;
		$.ajax({
			url : url,
			async : false
		}).done(function(data) {
			toReturn = data;
		});
		return toReturn;
	};
	// --------------------------------------------

	// --------------------------------------------
	// Rendering
	// --------------------------------------------

	function renderVisualization(observer,data) {
		checkObserver(observer,data)
		extern.stateChange(data)
	}
	
	function checkObserver(observer,data) {
		var observerList = observer.observer;
		if (observerList !== undefined) {
			for ( var i = 0; i < observerList.length; i++) {
				var observer = observerList[i];
				bms[observer.cmd](observer,data);
			}
		}
	}
	
	// --------------------------------------------

	extern.client = ""
	extern.observer = null;
	extern.init = session.init
	extern.session = session

	function browse(dir_dom) {
		$('#filedialog').off('hidden.bs.modal')
		$('#filedialog').on('hidden.bs.modal', set_ok_button_state(dir_dom))
		$("#filedialog").modal('show')
		browse2(dir_dom)
	}

	function set_ok_button_state(dir_dom) {
		return function() {
			var file = $(dir_dom)[0].value
			var valid = check_file(file);
			if (valid) {
				$("#fb_okbtn").removeAttr("disabled")
			} else {
				$("#fb_okbtn").attr("disabled", "disabled")
			}
		}
	}

	function browse2(dir_dom) {
		var dir = $(dir_dom)[0].value
		// prepare dialog
		var data = request_files(dir)
		$(dir_dom).val(data.path)
		filldialog(data.dirs, data.files, dir_dom)
	}

	function request_files(d) {
		var s;
		$.ajax({
			url : "/files?path=" + d + "&extensions=bms",
			success : function(result) {
				if (result.isOk === false) {
					alert(result.message);
				} else {
					s = JSON.parse(result);
				}
			},
			async : false
		});
		return s;
	}

	function check_file(d) {
		var s;
		$.ajax({
			url : "/files?check=true&path=" + d + "&extensions=bms",
			success : function(result) {
				if (result.isOk === false) {
					alert(result.message);
				} else {
					s = JSON.parse(result);
				}
			},
			async : false
		});
		return s;
	}

	function filldialog(dirs, files, dir_dom) {
		$(".filedialog_item").remove()
		$(".filedialog_br").remove()
		var hook = $("#filedialog_content")
		var s
		for (s in dirs) {
			var file = dirs[s]
			if (!file.hidden) {
				hook.append(session.render("/ui/bmsview/fb_dir_entry.html", {
					"name" : file.name,
					"path" : file.path,
					"dom" : dir_dom
				}))
			}
		}
		for (s in files) {
			var file = files[s]
			if (!file.hidden) {
				hook.append(session.render("/ui/bmsview/fb_file_entry.html", {
					"name" : file.name,
					"path" : file.path,
					"dom" : dir_dom
				}))
			}
		}
	}

	function fb_select_dir(dir_dom, path) {
		$(dir_dom).val(path)
		browse2(dir_dom)
	}
	function fb_select_file(dir_dom, path) {
		$(dir_dom).val(path)
		$("#filedialog").modal('hide')
	}

	extern.browse = browse
	extern.fb_select_dir = fb_select_dir
	extern.fb_select_file = fb_select_file
	extern.fb_load_file = function(dom_dir) {
		templateFile = $(dom_dir)[0].value
		session.sendCmd("setTemplate", {
			"path" : templateFile
		})
		$("#sourceModal").modal('hide')
		$("#chooseTemplateBox").css("display", "none");
	}

	extern.setTemplate = function(data) {
		window.location = "/bms/?template=" + data.request;
	}
	
	extern.reloadTemplate = function(data) {
		renderVisualization(JSON.parse(data.observer).wrapper, JSON.parse(data.data))
	}

	extern.renderVisualization = function(data) {
		renderVisualization(JSON.parse(data.observer).wrapper, JSON.parse(data.data))
	}
	
	extern.stateChange = function(data) {
	}
	
	extern.translateValue = function(val) {
		if (val === "true") {
			return true;
		} else if (val === "false") {
			return false;
		} else if(!isNaN(val)) {
			val = parseInt(val)
		}
		return val;
	}
	
	extern.evalObserver = function(observer,data) {

		var objects = observer.objects
		var results = data.eval	
		
		var evalFunc = function() {
			return function(text, render) {
				return results[text];
			}
		}
	
		data.eval = evalFunc
		 		
		for ( var i = 0; i < objects.length; i++) {

			var o = objects[i];
			var predicate = o.predicate;
			var triggerList = o.trigger;

			if(predicate === undefined) {
				predicate = true;
			} else {
				predicate = Mustache.render(predicate, {
					"eval" : evalFunc
				})
				predicate = extern.translateValue(predicate);
			}
			
			if(predicate) {
				
				for ( var t = 0; t < triggerList.length; t++) {
					
					var trigger = triggerList[t]			
					var parameters = trigger.parameters
					var caller = trigger.call
					
					if((parameters !== undefined) && (caller !== undefined)) {
						var parsedArray = [];
						$(parameters).each(function(k,v) {
							var fval = Mustache.render(v.toString(), data);
							fval = extern.translateValue(fval);
							parsedArray.push(fval)		
						});
						var obj = $(trigger.selector)
						var fn = obj[caller];
						if (typeof fn === "function") {
							fn.apply(obj, parsedArray);
						}
					}
					
				}
				
			}

		}

	}
	
	var bodyClone;
	
	extern.cspEventObserver = function(observer,data) {

		// Revert objects ...
		if(bodyClone) {
			$("body").replaceWith(bodyClone)
		}
		bodyClone = $("body").clone(true,true)	
		
		var objects = observer.objects
		var trace = data.model.trace
		var results = data.eval
		
		// Replay trace ...
		$.each(trace, function(i, l) {

			var lastop = l.full
			$.each(objects, function(i, o) {

				var result = Mustache.render(o.events, {
					"eval" : function() {
						return function(text, render) {
							return results[text];
						}
					}
				})
				
				if (result !== undefined) {

					if (result.indexOf(lastop) !== -1) {

						var trigger = o.trigger

						$.each(trigger, function(i, t) {

							var parameters = t.parameters
							var caller = t.call

							if ((parameters !== undefined)
									&& (caller !== undefined)) {
								var parsedArray = [];
								$(parameters).each(function(k, v) {
									parsedArray.push(extern.translateValue(Mustache.render(v,l)))
								});
								var obj = $(Mustache.render(t.selector, l))
								var fn = obj[caller];
								if (typeof fn === "function") {
									fn.apply(obj, parsedArray);
								}
							}

						});

					}

				}

			});

		});
		
//		console.log(data.eval)
//		objects = observer.objects
//		trace = data.model.trace		
//		var formulas = [];
//		$.each(observer.objects, function(i,o) {		
//			formulas.push(o.events)
//		});
//		session.sendCmd("eval", {
//			"formulas" : JSON.stringify(formulas),
//			"callback" : "bms.cspEventResult"
//		})
		
	}
	
	extern.executeOperation = function(observer,data) {
		
		  var objects = observer.objects
		  
		  $.each(objects, function(i,v)
		  {
			  var o = v;
			  var predicate = o.predicate;
			  if(predicate === undefined)
				  predicate = "1=1"
			  var operation = o.operation
			  var selector = $(o.selector);
			  
			  var events = $._data( selector[0], 'events' )
			  if (events === undefined || (events !== undefined && events.click === undefined)) {
				    selector.click(function() {
					 	session.sendCmd("executeOperation", {
							"op" : o.operation,
							"predicate" : predicate,
							"client" : parent.bms.client
						})	
				  });
			  }
			  
		  });
		  
		}

	return extern;

}())
