package com.example.yanglh6.myapplication9;

import java.io.Serializable;

/*  存储相应的数据  */
public class Weather {

    public Weather(String Date, String Weather_description, String Temperature) {
        this.date = Date;
        this.weather_description = Weather_description;
        this.temperature = Temperature;
    }

    public String getWeather_description() {
        return weather_description;
    }

    public void setWeather_description(String weather_description) {
        this.weather_description = weather_description;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTemperature() {
        return temperature;
    }

    public void setTemperature(String temperature) {
        this.temperature = temperature;
    }

    public String weather_description;
    public String date;
    public String temperature;
}