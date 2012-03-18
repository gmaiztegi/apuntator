define ['use!backbone', 'models/MenuItem'], (Backbone, MenuItem) ->
	class Menu extends Backbone.Collection
		model: MenuItem

		comparator: (one, two) ->
			return -1 if one.get('order') < two.get ('order')
			return 1 if one.get ('order') > two.get ('order')

			return -1 if one.get('tag') < two.get('tag')
			return 1 if one.get('tag') > two.get('tag')

			0