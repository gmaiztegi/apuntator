define ['marionette'], (Marionette) ->
	class MenuItem extends Marionette.ItemView
		template: '#tmpl-menu-item'
		tagName: 'li'

		onRender: ->
			@model.on('change:active', @setActive)

		setActive: (model, value) =>
			if value then @$el.addClass('active')
			else @$el.removeClass('active')