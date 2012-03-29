define ['backbone'], (Backbone) ->
	class MenuItem extends Backbone.Model
		defaults:
			order: 100
			active: false