import matplotlib.pyplot as plt
import numpy as np

def plot_simulation_results():
    """
    This function generates and saves bar charts for the FarmFog simulation results.
    It creates three plots: Execution Time, Total Energy Consumption, and Network Usage.
    """

    # Architecture labels for the two models
    labels = ['Fog-Based Model', 'Cloud-Only Model']
    
    # Metric 1: Execution Time
    execution_time = [127, 512]
    
    # Metric 2: Total Energy Consumption 
    energy_consumption_j = [1188872, 1046801]
    energy_consumption_mj = [j / 1_000_000 for j in energy_consumption_j] # convert to MegaJoules (MJ)

    # Metric 3: Network Usage (Bytes)
    network_usage_bytes = [6096, 6646]
    network_usage_kb = [b / 1024 for b in network_usage_bytes] # convert to KiloBytes (KB)

    bar_width = 0.5
    colors = ['#4A90E2', '#F5A623'] 
    
    # Chart 1 - Execution Time Comparison
    fig1, ax1 = plt.subplots(figsize=(8, 6))
    bars1 = ax1.bar(labels, execution_time, color=colors, width=bar_width)

    ax1.set_ylabel('Time (ms)')
    ax1.set_title('Execution Time Comparison', fontsize=16, fontweight='bold')
    ax1.set_ylim(0, max(execution_time) * 1.2) # Add 20% padding to the top

    # data labels on top of the bars
    for bar in bars1:
        yval = bar.get_height()
        ax1.text(bar.get_x() + bar.get_width()/2.0, yval + 10, f'{yval} ms', ha='center', va='bottom')

    plt.tight_layout()
    plt.savefig('execution_time_comparison.png')
    print("Saved 'execution_time_comparison.png'")
    plt.close(fig1)

    # Chart 2 - Total Energy Consumption
    fig2, ax2 = plt.subplots(figsize=(8, 6))
    bars2 = ax2.bar(labels, energy_consumption_mj, color=colors, width=bar_width)

    ax2.set_ylabel('Energy (MegaJoules)')
    ax2.set_title('Total Energy Consumption', fontsize=16, fontweight='bold')
    ax2.set_ylim(0, max(energy_consumption_mj) * 1.2)

    # data labels on top of the bars
    for bar in bars2:
        yval = bar.get_height()
        ax2.text(bar.get_x() + bar.get_width()/2.0, yval + 0.02, f'{yval:.2f} MJ', ha='center', va='bottom')

    plt.tight_layout()
    plt.savefig('total_energy_consumption.png')
    print("Saved 'total_energy_consumption.png'")
    plt.close(fig2)

    # Chart 3 - Network Usage Comparison
    fig3, ax3 = plt.subplots(figsize=(8, 6))
    bars3 = ax3.bar(labels, network_usage_kb, color=colors, width=bar_width)

    ax3.set_ylabel('Data Transferred (KB)')
    ax3.set_title('Total Network Usage (LoRaWAN-WAN)', fontsize=16, fontweight='bold')
    ax3.set_ylim(0, max(network_usage_kb) * 1.2)

    # data labels on top of the bars
    for bar in bars3:
        yval = bar.get_height()
        ax3.text(bar.get_x() + bar.get_width()/2.0, yval + 0.1, f'{yval:.2f} KB', ha='center', va='bottom')

    plt.tight_layout()
    plt.savefig('network_usage_comparison.png')
    print("Saved 'network_usage_comparison.png'")
    plt.close(fig3)

if __name__ == '__main__':
    plot_simulation_results()
