package com.dan.eo1;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class FlickrGetSizesResponse {

    @SerializedName("sizes")
    private Sizes sizes;

    public Sizes getSizes() {
        return sizes;
    }

    public void setSizes(Sizes sizes) {
        this.sizes = sizes;
    }

    public class Sizes {
        @SerializedName("size")
        private List<FlickrImageSize> imageSizes;

        public List<FlickrImageSize> getImageSizes() {
            return imageSizes;
        }

        public void setImageSizes(List<FlickrImageSize> imageSizes) {
            this.imageSizes = imageSizes;
        }
    }

    public class FlickrImageSize {
        @SerializedName("label")
        private String label;

        @SerializedName("source")
        private String sourceUrl;

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getSourceUrl() {
            return sourceUrl;
        }

        public void setSourceUrl(String sourceUrl) {
            this.sourceUrl = sourceUrl;
        }
    }
}

