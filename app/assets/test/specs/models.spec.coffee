define ['models/File', 'models/MenuItem'], (File, MenuItem) ->

    describe "File model spec", ->

    describe "MenuItem model spec", ->
        it "default order is 100", ->
            item = new MenuItem
            expect(item.get("order")).toBe(100)
        it "is not active by default", ->
            item = new MenuItem
            expect(item.get("active")).toBe(false)