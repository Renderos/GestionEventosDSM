package com.example.gestioeventosdsm

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.example.gestioeventosdsm.databinding.FragmentFirstBinding

/**
 * FirstFragment: default destination fragment defined in the Navigation Graph.
 */
class FirstFragment : Fragment() {

    // ViewBinding reference (nullable). Only valid between onCreateView and onDestroyView.
    private var _binding: FragmentFirstBinding? = null

    // Non-null accessor for binding. Safe to use only when the view exists.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        // Inflate the layout using ViewBinding
        _binding = FragmentFirstBinding.inflate(inflater, container, false)

        // Return the root view of the inflated layout
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Button click listener → navigate to SecondFragment using Navigation Component
        binding.buttonFirst.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Avoid memory leaks by clearing the binding reference
        _binding = null
    }
}
