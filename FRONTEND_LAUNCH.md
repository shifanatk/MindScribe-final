# 🚀 MindScribe Frontend Launch Guide

## 📋 Prerequisites:
1. ✅ Backend running on localhost:8080
2. ✅ Java 17+ installed
3. ✅ Maven configured

## 🎯 Launch Commands:

### **Option 1: Backend + Frontend (Recommended)**
```bash
# Terminal 1: Start Backend
mvn spring-boot:run -P backend

# Terminal 2: Start Frontend  
mvn javafx:run -P frontend
```

### **Option 2: Frontend Only (if backend already running)**
```bash
mvn javafx:run -P frontend
```

### **Option 3: Direct JavaFX Launch**
```bash
mvn clean compile javafx:run
```

## 🎨 What You'll See:

### **Ethereal Glassmorphism Design:**
- **Soft Porcelain Background** (#F8F9FB)
- **Serene Peri Accents** (#A5B4FC) 
- **Dawn Peach AI Highlights** (#FBCFE8)
- **Frosted Glass Effects** with 70% opacity
- **18px Border Radius** throughout
- **Smooth Animations** on all interactions

### **Layout Components:**
- **Left Sidebar**: Icon navigation (Home, Journal, Analytics, AI, Settings, Profile)
- **Center Editor**: Large glass-textarea for journal writing
- **Right Panel**: Mood charts, sentiment trends, recent entries, AI insights
- **AI Integration**: "AI Analyze" button with sentiment results

### **Interactive Features:**
- **Fade-in Entrance Animation**: Smooth 0.8s fade on startup
- **Slide-in Sidebar**: Left navigation slides in from left
- **Hover Effects**: All buttons scale and glow on hover
- **Real-time Charts**: Pie chart for mood distribution, area chart for trends
- **AI Sentiment**: Analyzes text and shows sentiment with confidence scores

## 🔧 Customization:

### **Change Colors**: Edit `src/main/resources/styles/style.css`
```css
.root {
    -fx-background-color: #YOUR_COLOR;
}
```

### **Add Icons**: Place 24x24 PNG icons in `src/main/resources/images/`
- home-icon.png
- journal-icon.png  
- analytics-icon.png
- ai-icon.png
- settings-icon.png
- profile-icon.png

### **Modify Layout**: Edit `src/main/resources/fxml/main-view.fxml`

## 🎯 Next Steps:
1. Add the missing icon files
2. Test the AI integration with your backend
3. Customize the color scheme to your preference
4. Add more features to the MainController

## 🌟 Design Philosophy:
- **Calm & Soft**: Pastel colors, rounded corners, gentle shadows
- **Professional**: Clean typography, consistent spacing
- **Modern**: Glassmorphism, smooth animations, subtle interactions
- **Accessible**: High contrast, clear hierarchy, intuitive navigation

Your MindScribe frontend is ready to launch! 🎨✨
