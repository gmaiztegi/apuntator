define ['backbone', 'jquery', 'handlebars', 'text!templates/file/row.html'],
(Backbone, $, Handlebars, rowTemplate) ->
        class FileView extends Backbone.View
            tagName: "tr"
            template: Handlebars.compile rowTemplate
            initialize: ->
                @model.bind('change', @render)
                @model.bind('destroy', @render)

            render: =>
                @$el.html @template(@model.toJSON())
                this

            clear: ->
                @model.destroy()