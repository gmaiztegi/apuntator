define [
	'use!marionette'
	'collections/Menu'
	'views/MenuItem'
], (Marionette, Menu, MenuItem) ->

	class MenuColView extends Marionette.CollectionView
		tagName: 'ul'
		className: 'nav'
		itemView: MenuItem

	class MenuView extends Marionette.Layout
		template: '#tmpl-menu'

		regions:
			left: 'div.left-menu'

		initialize: ->
			@leftitems = new Menu
			@leftmenu = new MenuColView {
				collection: @leftitems
			}

		onRender: ->
			@left.show @leftmenu


	new MenuView