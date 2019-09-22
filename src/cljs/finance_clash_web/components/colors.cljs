(ns finance-clash-web.components.colors
  (:require [goog.string]
            [goog.string.format]
            [goog.color]))

(def format goog.string.format)

(def colors
  {:graphite [55 58 54]
   :main [55 58 54]
   :sand [225 220 202]
   :silver [208 209 209]
   :white [255 255 255]
   ;; :evolution colors

   :aquamarine [119 197 213]
   :citrine [255 212 0]
   :coral [241 86 123]
   :nucleus-blue [0 98 152]
   :emerald [0 164 153]
   :amethyst [112 32 130]
   :amber [237 139 0]

   ;; ::base color tints
   :sand-bright [240 237 228]
   :sand-dark [158 154 142]
   :silver-bright [227 227 227]
   :silver-dark [146 147 147]

   ;; ::evolution color tints
   :aquamarine-bright [173 220 230]
   :aquamarine-dark [84 138 150]
   :citrine-bright [255 229 102]
   :citrine-dark [204 170 0]
   :coral-bright [248 170 189]
   :coral-dark [169 60 86]
   :nucleus-blue-bright [127 176 203]
   :nucleus-blue-dark [0 69 107]
   :emerald-bright [127 209 204]
   :emerald-dark [0 115 106]
   :amethyst-bright [183 143 192]
   :amethyst-dark [79 22 91]
   :amber-bright [244 185 102]
   :amber-dark [166 98 0]

   ;; ::trafic colors
   :green-light [0 154 23]
   :yellow-light [255 205 0]
   :red-light [213 0 50]
   :green-light-bright [76 184 92]
   :green-light-dark [0 123 18]
   :red-light-bright [226 76 94]
   :red-light-dark [170 0 40]

   ;; defines primary and secondary colors to avoid refactor
   :primary [255 212 0] ;; citrine
   :secondary [241 86 123] ;; coral
})

(defn rgb->str [v] (apply format "rgb(%d, %d, %d)" v))
(defn rgb->hex [v] (apply goog.color/rgbToHex v))
(def color->str (comp rgb->str colors))
(def colors-rgb (reduce-kv #(assoc %1 %2 (rgb->str %3)) {} colors))
(def colors-hex (reduce-kv #(assoc %1 %2 (rgb->hex %3)) {} colors))


(def palettes
  (let [simple [:graphite :aquamarine :citrine :coral
                :nucleus-blue :emerald :amethyst :amber]]
    {:simple simple
     :contrast (into [:graphite]
                     (mapcat #(vec [(keyword (str (name %) "-bright"))
                                    (keyword (str (name %) "-dark"))]) (rest simple)))
     :full (reduce
            (fn [xs k]
              (into xs (mapv #(keyword (str (name %) k)) (rest simple))))
            [:graphite] ["" "-bright" "-dark"])}))

(def palettes-rgb (reduce-kv #(assoc %1 %2 (mapv colors-rgb %3)) {} palettes))
(def palettes-hex (reduce-kv #(assoc %1 %2 (mapv colors-hex %3)) {} palettes))

(def simple-palette (:simple palettes-rgb))
(def simple-palette-hex (:simple palettes-hex))

(def full-palette (:full palettes-rgb))
(def full-palette-hex (:full palettes-hex))

(defn positive-negative-color [d]
  (if (>= d 0)
    (colors-hex :green-light)
    (colors-hex :red-light)))
