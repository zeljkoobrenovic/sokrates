/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

ace.define("ace/snippets/fsharp",["require","exports","module"], function(require, exports, module) {
    "use strict";

    exports.snippetText =undefined;
    exports.scope = "fsharp";

});                (function() {
                    ace.require(["ace/snippets/fsharp"], function(m) {
                        if (typeof module == "object" && typeof exports == "object" && module) {
                            module.exports = m;
                        }
                    });
                })();
